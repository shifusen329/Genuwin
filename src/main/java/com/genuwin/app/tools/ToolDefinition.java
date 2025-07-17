package com.genuwin.app.tools;

import java.util.Map;

/**
 * Represents a tool definition with metadata for the new tools system
 */
public class ToolDefinition {
    private String name;
    private String description;
    private Map<String, ParameterDefinition> parameters;
    private String category;
    private boolean requiresConfirmation;
    
    public ToolDefinition() {}
    
    public ToolDefinition(String name, String description, Map<String, ParameterDefinition> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.category = "general";
        this.requiresConfirmation = false;
    }
    
    public ToolDefinition(String name, String description, Map<String, ParameterDefinition> parameters, 
                         String category, boolean requiresConfirmation) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.category = category;
        this.requiresConfirmation = requiresConfirmation;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, ParameterDefinition> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, ParameterDefinition> parameters) {
        this.parameters = parameters;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }
    
    public void setRequiresConfirmation(boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }
    
    /**
     * Generate a description for the system prompt
     */
    public String getSystemPromptDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(name).append(": ").append(description);
        
        if (parameters != null && !parameters.isEmpty()) {
            sb.append(" (Parameters: ");
            boolean first = true;
            for (Map.Entry<String, ParameterDefinition> entry : parameters.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append(": ").append(entry.getValue().getDescription());
                first = false;
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Parameter definition for tools
     */
    public static class ParameterDefinition {
        private String type;
        private String description;
        private boolean required;
        private Object defaultValue;
        
        public ParameterDefinition() {}
        
        public ParameterDefinition(String type, String description, boolean required) {
            this.type = type;
            this.description = description;
            this.required = required;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public void setRequired(boolean required) {
            this.required = required;
        }
        
        public Object getDefaultValue() {
            return defaultValue;
        }
        
        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}
