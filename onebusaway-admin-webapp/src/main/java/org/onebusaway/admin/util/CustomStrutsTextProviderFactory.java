/**
 * Copyright (C) 2021 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
