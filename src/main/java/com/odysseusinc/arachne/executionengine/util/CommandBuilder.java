package com.odysseusinc.arachne.executionengine.util;

import java.util.ArrayList;
import java.util.List;

public class CommandBuilder {

    List<String> statements = new ArrayList<>();

    private CommandBuilder(){
    }

    public static CommandBuilder newCommand(){
        return new CommandBuilder();
    }

    public CommandBuilder statement(String statement){
        statements.add(statement);
        return this;
    }

    public CommandBuilder withParam(String param){
        statements.add(param);
        return this;
    }

    public String[] build(){
        return statements.toArray(new String[0]);
    }
}
