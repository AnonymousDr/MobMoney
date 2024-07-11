package com.anderhurtado.spigot.mobmoney.objets;

public class PreconfiguredCommand {

    private final String command;
    private final ExecutionType executionType;

    public PreconfiguredCommand(String command, ExecutionType executionType) throws NullPointerException{
        if(command == null) throw new NullPointerException("Command cannot be null!");
        if(executionType == null) throw new NullPointerException("ExecutionType cannot be null!");
        this.command = command;
        this.executionType = executionType;
    }

    public String getCommand() {
        return command;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }


    public enum ExecutionType {
        PLAYER, PLAYEROP, CONSOLE;

        /**
         * Gets the execution type
         * @param name Execution type.
         * @return Execution type found. If none, will return PLAYER by default.
         */
        public static ExecutionType getByName(String name) {
            for(ExecutionType e:values()) {
                if(e.name().equalsIgnoreCase(name)) return e;
            }
            return PLAYER;
        }

    }

}
