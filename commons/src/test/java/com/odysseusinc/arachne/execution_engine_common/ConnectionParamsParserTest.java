package com.odysseusinc.arachne.execution_engine_common;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.execution_engine_common.util.ConnectionParams;
import com.odysseusinc.arachne.execution_engine_common.util.ConnectionParamsParser;
import org.junit.Assert;
import org.junit.Test;

public class ConnectionParamsParserTest {

    @Test
    public void testParsingParamWithEmptyValue() {

        ConnectionParams connectionParams = ConnectionParamsParser.parse(DBMSType.POSTGRESQL, "jdbc:postgresql://localhost:64290/postgres?binaryTransferEnable=&unknownLength=2147483647");
        Assert.assertEquals("Extra params parsed properly", connectionParams.getExtraSettings(), "binaryTransferEnable=&unknownLength=2147483647");
    }
}
