package com.taobao.tddl.common.extension;

import com.taobao.tddl.common.utils.extension.ExtensionLoader;
import org.junit.Assert;
import org.junit.Test;

public class ExtensionLoaderTest {

    @Test
    public void testSimple() {
        AbstractPluginService plugin = ExtensionLoader.load(AbstractPluginService.class);
        Assert.assertEquals(plugin.getClass(), OrderedPlugin.class);
        Assert.assertEquals(plugin.echo("hello"), "ordered : hello");
    }
}
