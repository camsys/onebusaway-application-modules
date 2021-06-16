package org.onebusaway.admin.util;

import com.opensymphony.xwork2.*;
import com.opensymphony.xwork2.inject.Inject;

import java.util.ResourceBundle;

public class CustomStrutsTextProviderFactory implements TextProviderFactory {
    protected LocaleProviderFactory localeProviderFactory;
    protected LocalizedTextProvider localizedTextProvider;

    @Inject
    public CustomStrutsTextProviderFactory(LocaleProviderFactory localeProviderFactory, LocalizedTextProvider localizedTextProvider) {
        this.localeProviderFactory = localeProviderFactory;
        this.localizedTextProvider = localizedTextProvider;

        this.localizedTextProvider.addDefaultResourceBundle("messages/label");
        this.localizedTextProvider.addDefaultResourceBundle("messages/customerA/label");

    }

    @Override
    public TextProvider createInstance(Class clazz) {
        TextProvider instance = getTextProvider(clazz);
        if (instance instanceof ResourceBundleTextProvider) {
            ((ResourceBundleTextProvider) instance).setClazz(clazz);
            ((ResourceBundleTextProvider) instance).setLocaleProvider(localeProviderFactory.createLocaleProvider());
        }
        return instance;
    }

    @Override
    public TextProvider createInstance(ResourceBundle bundle) {
        TextProvider instance = getTextProvider(bundle);
        if (instance instanceof ResourceBundleTextProvider) {
            ((ResourceBundleTextProvider) instance).setBundle(bundle);
            ((ResourceBundleTextProvider) instance).setLocaleProvider(localeProviderFactory.createLocaleProvider());
        }
        return instance;
    }

    protected TextProvider getTextProvider(Class clazz) {
        return new TextProviderSupport(clazz, localeProviderFactory.createLocaleProvider(), localizedTextProvider);
    }

    protected TextProvider getTextProvider(ResourceBundle bundle) {
        return new TextProviderSupport(bundle, localeProviderFactory.createLocaleProvider(), localizedTextProvider);
    }
}
