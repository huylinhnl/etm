package com.jecstar.etm.gui.settings.users;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {
                "classpath:features/settings/users.feature"
        })
public class UsersIntegrationTest {
}
