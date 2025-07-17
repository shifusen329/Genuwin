package com.genuwin.app.api.models;

/**
 * Model class for tool calls in Ollama responses
 */
public class ToolCall {
    private String id;
    private String type;
    private Function function;
    
    public ToolCall() {}
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Function getFunction() {
        return function;
    }
    
    public void setFunction(Function function) {
        this.function = function;
    }
    
    public static class Function {
        private String name;
        private Object arguments;
        
        public Function() {}
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Object getArguments() {
            return arguments;
        }
        
        public void setArguments(Object arguments) {
            this.arguments = arguments;
        }
        
        public String getArgumentsAsString() {
            if (arguments instanceof String) {
                return (String) arguments;
            } else {
                return new com.google.gson.Gson().toJson(arguments);
            }
        }
    }
}
