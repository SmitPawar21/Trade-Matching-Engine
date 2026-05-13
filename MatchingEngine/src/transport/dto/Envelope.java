package transport.dto;

import transport.dto.EngineRequest;

public class Envelope {
	private String type;
    private EngineRequest data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EngineRequest getData() {
        return data;
    }

    public void setData(EngineRequest data) {
        this.data = data;
    }
}
