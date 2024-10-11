/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Anastasiia Klochkova
 * Created: September 26, 2018
 *
 */

package com.odysseusinc.datasourcemanager.krblogin;

import com.github.jknack.handlebars.Template;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.KerberosAuthMechanism;
import com.odysseusinc.datasourcemanager.TemplateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class KerberosServiceImpl implements KerberosService {
    private static final String LOG_FILE = "kinit_out.txt";
    public static final String KINIT_COMMAND = "kinit";
    private final static String REALMS = "[realms]";
    private final static String DOMAIIN_REALM = "[domain_realm]";
    private long timeout;
    private String kinitPath;
    private String configPath;

    public KerberosServiceImpl(long timeout, String kinitPath, String configPath) {
        this.timeout = timeout;
        this.kinitPath = kinitPath;
        this.configPath = configPath;
    }

    @Override
    public KrbConfig runKinit(DataSourceUnsecuredDTO dataSource, RuntimeServiceMode environmentMode, File workDir) throws IOException {

        KrbConfig krbConfig = prepareToKinit(dataSource, environmentMode);

        File stdout = new File(workDir, LOG_FILE);
        ProcessBuilder pb = new ProcessBuilder();
        Process process = pb.directory(workDir)
                .redirectOutput(ProcessBuilder.Redirect.to(stdout))
                .redirectError(ProcessBuilder.Redirect.appendTo(stdout))
                .command(krbConfig.getComponents().getKinitCommand()).start();
        try {
            process.waitFor(timeout, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                log.warn("kinit exit code: {}", process.exitValue());
            }
            process.destroy();
            if (log.isDebugEnabled()) {
                klist(workDir);
            }
        } catch (InterruptedException e) {
            log.error("Failed to obtain kerberos ticket", e);
        }
        return krbConfig;
    }

    private synchronized KrbConfig prepareToKinit(DataSourceUnsecuredDTO dataSource, RuntimeServiceMode environmentMode) throws IOException {

        KrbConfig krbConfig = new KrbConfig();
        //it is needed to extend config on host regardless of current RuntimeServiceMode for successful detectCdmVersion() because it uses non-jail config
        Path path = extendKrbConf(Paths.get(this.configPath), dataSource);

        if (RuntimeServiceMode.ISOLATED == environmentMode) {
            krbConfig.setConfPath(buildTempKrbConf(dataSource));
        } else {
            krbConfig.setConfPath(path);
        }
        krbConfig.setMode(environmentMode);

        Path keytabPath = Paths.get("");
        if (dataSource.getKrbAuthMethod().equals(KerberosAuthMechanism.KEYTAB)) {
            if (Objects.isNull(dataSource.getKeyfile())) {
                throw new IllegalArgumentException("Kerberos keytab file is required for KEYTAB authentication");
            }
            keytabPath = Files.createTempFile("", ".keytab");
            try (OutputStream out = new FileOutputStream(keytabPath.toFile())) {
                IOUtils.write(dataSource.getKeyfile(), out);
            }
        }

        KinitComponents components = buildKinitCommand(dataSource, keytabPath);
        krbConfig.setComponents(components);

        return krbConfig;
    }

    private KinitComponents buildKinitCommand(DataSourceUnsecuredDTO dataSource, Path keytab) {

        CommandBuilder builder = CommandBuilder.newCommand();
        if (StringUtils.isBlank(dataSource.getKrbUser())) {
            throw new IllegalArgumentException("Kerberos user is required for authentication");
        }
        switch (dataSource.getKrbAuthMethod()) {
            case PASSWORD:
                if (SystemUtils.IS_OS_UNIX) {
                    if (StringUtils.isBlank(dataSource.getKrbPassword())) {
                        throw new IllegalArgumentException("Kerberos password is required for PASSWORD authentication");
                    }
                    builder.statement("bash")
                            .withParam("-c")
                            .statement("echo " + dataSource.getKrbPassword() + " | " + kinitPath + KINIT_COMMAND + " " +
                                    dataSource.getKrbUser() + "@" + dataSource.getKrbRealm());
                } else if (SystemUtils.IS_OS_WINDOWS) {
                    //todo: implement https://github.com/Waffle/waffle solution for this case
                    throw new RuntimeException("PASSWORD authentication is forbidden for Windows, use KEYTAB instead");
                }
                break;
            case KEYTAB:
                builder.statement(kinitPath + KINIT_COMMAND)
                        .withParam("-k")
                        .withParam("-t")
                        .withParam(keytab.toString())
                        .withParam(dataSource.getKrbUser() + "@" + dataSource.getKrbRealm());
                break;
            default:
                throw new IllegalArgumentException("Unsupported authentication type");
        }
        return createKinitComponents(dataSource, builder.build(), keytab, kinitPath);
    }

    private KinitComponents createKinitComponents(DataSourceUnsecuredDTO dataSource, String[] kinitCommand, Path keytab, String kinitPath) {
        KinitComponents components = new KinitComponents();
        components.setKrbPassword(dataSource.getKrbPassword());
        components.setKrbUser(dataSource.getKrbUser());
        components.setKrbRealm(dataSource.getKrbRealm());
        components.setKinitCommand(kinitCommand);
        components.setKeytabPath(keytab);
        components.setAuthMechanism(dataSource.getKrbAuthMethod());
        components.setKinitPath(kinitPath);
        return components;
    }

    private Path buildTempKrbConf(DataSourceUnsecuredDTO dataSource) throws IOException {

        Path configPath = Files.createTempFile("", ".conf");
        String krbConfHeader = buildKrbConfHeader(dataSource.getKrbRealm());
        FileUtils.write(configPath.toFile(), krbConfHeader, UTF_8);
        extendKrbConf(configPath, dataSource);
        return configPath;
    }

    private Path extendKrbConf(Path configPath, DataSourceUnsecuredDTO dataSource) throws IOException {

        File config = configPath.toFile();
        long fileLength = config.length();
        try (RandomAccessFile raf = new RandomAccessFile(config, "rw")) {
            String confStr = convertConfigToString(raf, (int) fileLength);

            //remove inconsistent part (if it exists) with old values of admin_server or kdc
            confStr = removeInconsistentRealm(dataSource, confStr);
            boolean isRealmDefined = confStr.toLowerCase().contains(" " + dataSource.getKrbRealm().toLowerCase() + " = {");
            if (!isRealmDefined) {
                confStr = addNewRealm(dataSource, confStr);
            }

            //remove inconsistent part (if it exists) with old values of pair domain-realm
            confStr = removeInconsistentDomainRealm(dataSource, confStr);
            boolean isDomainDefined = confStr.toLowerCase().contains(" " + dataSource.getKrbFQDN() + " = ");
            if (!isDomainDefined) {
                confStr = addNewDomainRealm(dataSource, confStr);
            }

            if (!isRealmDefined || !isDomainDefined) {
                confStr = confStr.trim();
                int confLength = confStr.length();
                raf.seek(0);
                raf.write(confStr.getBytes());
                raf.setLength(confLength);
            }
        }
        removeEmptyLines(configPath);
        return configPath;
    }

    private void removeEmptyLines(Path configPath) throws IOException {

        List<String> lines = FileUtils.readLines(configPath.toFile(), "UTF-8");
        lines.removeIf(line -> line.trim().isEmpty());
        FileUtils.writeLines(configPath.toFile(), lines);
    }

    private String addNewRealm(DataSourceUnsecuredDTO dataSource, String confStr) throws IOException {

        String newRealm = buildKrbConfRealm(dataSource);
        int startPosition = confStr.indexOf(REALMS) + REALMS.length();
        return confStr.replace(confStr.substring(0, startPosition), confStr.substring(0, startPosition) + newRealm);
    }

    private String addNewDomainRealm(DataSourceUnsecuredDTO dataSource, String confStr) {

        String newDomain = "  " + dataSource.getKrbFQDN() + " = " + dataSource.getKrbRealm();
        if (!confStr.contains(DOMAIIN_REALM)) {
            confStr += System.lineSeparator() + DOMAIIN_REALM + System.lineSeparator();
        }
        int startPosition = confStr.indexOf(DOMAIIN_REALM) + DOMAIIN_REALM.length();
        return confStr.replace(confStr.substring(0, startPosition), confStr.substring(0, startPosition) + System.lineSeparator() + newDomain);
    }

    private String removeInconsistentDomainRealm(DataSourceUnsecuredDTO dataSource, String confStr) {

        String tmpConf = confStr;
        Pattern realmPattern = Pattern.compile(".*?( " + dataSource.getKrbFQDN() + " = (\\S*)\\s*?)\\S*.*", Pattern.DOTALL);
        Matcher realmMatcher = realmPattern.matcher(confStr.toLowerCase());
        if (realmMatcher.matches() && !(realmMatcher.group(2).equalsIgnoreCase(dataSource.getKrbRealm()))) {
            tmpConf = tmpConf.replaceAll("(?i)" + Pattern.quote(realmMatcher.group(1)), "  ");
        }
        return tmpConf;
    }

    private String removeInconsistentRealm(DataSourceUnsecuredDTO dataSource, String confStr) {

        String tmpConf = confStr;
        Pattern pattern = Pattern.compile(".*?( " + dataSource.getKrbRealm().toLowerCase() + " = \\{\\s*?admin_server = (.*?)\\s*?kdc = (.*?)\\s*?}.*?)\\S.*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(confStr.toLowerCase());
        if (matcher.matches() && (!matcher.group(2).equalsIgnoreCase(dataSource.getKrbAdminFQDN()) || !matcher.group(3).equalsIgnoreCase(dataSource.getKrbFQDN()))) {
            tmpConf = tmpConf.replaceAll("(?i)" + Pattern.quote(matcher.group(1)), "  ");
        }
        return tmpConf;
    }

    private String buildKrbConfHeader(String defaultRealmName) throws IOException {

        return TemplateUtils
                .loadTemplate("/templates/krb5ConfHeader.mustache")
                .apply(Collections.singletonMap("defaultRealmName", defaultRealmName));
    }

    private String buildKrbConfRealm(DataSourceUnsecuredDTO dataSource) throws IOException {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("realmName", dataSource.getKrbRealm());
        parameters.put("adminServer", dataSource.getKrbAdminFQDN());
        parameters.put("kdcServer", dataSource.getKrbFQDN());

        Template confTemplate = TemplateUtils.loadTemplate("/templates/krb5ConfRealm.mustache");
        return confTemplate.apply(parameters);
    }

    private String convertConfigToString(RandomAccessFile raf, int fileLength) throws IOException {

        raf.seek(0);
        byte[] fileBytes = new byte[fileLength];
        raf.readFully(fileBytes);
        return new String(fileBytes);
    }

    private void klist(File workDir) {

        File stdout = new File(workDir, LOG_FILE);
        try {
            Process process = new ProcessBuilder()
                    .directory(workDir)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(stdout))
                    .redirectError(ProcessBuilder.Redirect.appendTo(stdout))
                    .command("klist")
                    .start();
            process.waitFor(timeout, TimeUnit.SECONDS);
            process.destroy();
        } catch (IOException | InterruptedException ignored) {
        }
    }
}
