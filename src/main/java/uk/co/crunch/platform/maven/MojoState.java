package uk.co.crunch.platform.maven;

import com.google.common.base.MoreObjects;
import org.jtwig.JtwigModel;

import java.nio.file.Path;

public class MojoState {
    private JtwigModel dataModel;

    public void setDataModel(final JtwigModel dataModel) {
        this.dataModel = dataModel;
    }

    public JtwigModel getDataModel() {
        return dataModel;
    }

    private static class Call {
        private final String type;
        private final String name;
        private final Path fileName;

        Call(String type, String name, Path fileName) {
            this.type = type;
            this.name = name;
            this.fileName = fileName;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(type)
                    .add("name", name)
                    .add("class", fileName)
                    .toString();
        }
    }
}
