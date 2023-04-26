package Crawl;

import org.jsoup.Connection;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Formular {
    private URL url;
    private URL actionUrl;
    private Connection.Method method = Connection.Method.GET;
    private List<FormularField> fields;

    public Formular() {
        fields = new ArrayList<>();
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public List<FormularField> getFields() {
        return fields;
    }

    public void setFields(List<FormularField> fields) {
        this.fields = fields;
    }

    public URL getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(URL actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Formular: ");
        sb.append(url);
        sb.append("\n");
        sb.append("Action: ");
        sb.append(actionUrl);
        sb.append("\n");
        sb.append("Fields: ");
        for (FormularField field : fields) {
            sb.append(field.getName());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * generate hashcode
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actionUrl == null) ? 0 : actionUrl.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Formular formular = (Formular) o;
        return Objects.equals(url, formular.url) && Objects.equals(actionUrl, formular.actionUrl) && method == formular.method && Objects.equals(fields, formular.fields);
    }

    public Connection.Method getMethod() {
        return method;
    }

    public void setMethod(String method) {
        if (method.equalsIgnoreCase("post")) {
            this.method = Connection.Method.POST;
        }
    }
}