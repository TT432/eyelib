package io.github.tt432.eyelib.util.data_attach;

public class DataAttachment<C> {

    private final DataAttachmentType<C> type;

    private C data;

    public DataAttachment(DataAttachmentType<C> type) {
        this(type, type.factory().get());
    }

    public DataAttachment(DataAttachmentType<C> type, C data) {
        this.type = type;
        this.data = data;
    }

    public DataAttachmentType<C> getType() {
        return type;
    }

    public C getData() {
        return data;
    }

    public void setData(C data) {
        this.data = data;
    }
}
