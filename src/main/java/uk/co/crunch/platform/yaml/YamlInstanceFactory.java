package uk.co.crunch.platform.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

/**
 * Completely glosses over the fact that Yaml's aren't thread-safe
 */
public class YamlInstanceFactory {

    public static Yaml create() {
        DumperOptions d = new DumperOptions();
        d.setDefaultFlowStyle(FlowStyle.BLOCK);
        d.setPrettyFlow(true);

        final Yaml yamlInst = new Yaml(d);
        yamlInst.setBeanAccess(BeanAccess.FIELD);
        return yamlInst;
    }
}
