
package com.odysseusinc.arachne.executionengine.api.v1;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.executionengine.service.AnalysisService;
import com.odysseusinc.arachne.executionengine.util.AnalisysUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import javax.validation.Valid;
import net.lingala.zip4j.exception.ZipException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@Api
@RequestMapping(value = AnalisysController.REST_API_MAIN)
public class AnalisysController {

    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_MAIN = "/api/v1";
    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_ANALYZE = "/analyze";
    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_METRICS = "/metrics";

    private final AnalysisService analysisService;

    @Autowired
    public AnalisysController(AnalysisService analysisService) {

        this.analysisService = analysisService;
    }

    @ApiOperation(value = "Files for analysis")
    @RequestMapping(value = REST_API_ANALYZE,
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public AnalysisRequestStatusDTO analyze(
            @RequestPart("analysisRequest") @Valid AnalysisRequestDTO analysisRequest,
            @RequestPart("file") List<MultipartFile> files,
            @RequestHeader(value = "arachne-compressed", defaultValue = "false") Boolean compressed,
            @RequestHeader(value = "arachne-waiting-compressed-result", defaultValue = "false") Boolean waitCompressedResult,
            @RequestHeader(value = "arachne-result-chunk-size-mb", defaultValue = "10485760") Long chunkSize
    ) throws FileNotFoundException, ZipException {

        final File analysisDir = AnalisysUtils.extractFiles(files, compressed);
        return analysisService.analyze(analysisRequest, analysisDir, waitCompressedResult, chunkSize);
    }

    @ApiOperation(value = "Prometheus compatible metrics")
    @RequestMapping(value = REST_API_METRICS,
            method = RequestMethod.GET,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String metrics() {

        int busy = analysisService.activeTasks();
        return "busy " + busy;
    }

}
