package pages.modules;

import java.util.List;

public final class HomepageModules {
    private HomepageModules() { }

    public static List<HomepageModule> all() {
        return List.of(
                new RulesModule(), new AreaMapModule(), new SchemeStagesModule(),
                new CriteriaSamplesModule(), new SlumListModule(), new RegisteredDevelopersModule(),
                new RtsLoginModule(), new RehabilitationOrdersModule(), new EligibilityListModule(),
                new SchemeApprovalModule(), new RemovalOrdersModule(), new BuildingPermissionsModule(),
                new HousingSocietiesModule(), new OccupancyCertificatesModule(), new LotteryDistributionModule()
        );
    }
}
