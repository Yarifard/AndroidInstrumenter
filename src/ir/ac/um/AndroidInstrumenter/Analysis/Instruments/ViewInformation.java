package ir.ac.um.AndroidInstrumenter.Analysis.Instruments;

public class ViewInformation {
    private String title;
    private String viewId;
    private String viewTag;
    private String viewType;
    private String bindingName;
    private String contentDescription;
    public ViewInformation(){
        this.title = "";
        this.viewId = "";
        this.viewType = "";
        this.viewTag = "";
        this.bindingName = "";
        contentDescription = "";
    };

    public String getTitle() {
        return title;
    }

    public String getViewId() {
        return viewId;
    }

    public String getViewType() {
        return viewType;
    }

    public String getTag(){ return viewTag;}

    public String getBindingName(){
        return this.bindingName;
    }

    public String getContentDesciption(){
        return contentDescription;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setViewId(String viewId) {
        this.viewId = viewId;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public void setViewTag(String viewTag){ this.viewTag = viewTag;}

    public void setBindingName(String bindingName){
        this.bindingName = bindingName;
    }

    public void setContentDesciption(String contentDescription) {
        this.contentDescription = contentDescription;
    }
}
