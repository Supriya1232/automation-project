package pages.modules;

import java.util.List;

public final class AreaMapModule extends HomepageModule {
    @Override public String label() { return "कार्याक्षेत्राचा नकाशा"; }
    @Override protected List<String> labelAliases() {
        return List.of(label(), "कार्यक्षेत्राचा नकाशा", "कार्य क्षेत्राचा नकाशा", "कार्यक्षेत्र नकाशा");
    }
}
