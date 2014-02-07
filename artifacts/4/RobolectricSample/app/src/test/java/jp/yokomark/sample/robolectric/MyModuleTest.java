package jp.yokomark.sample.robolectric;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class) // TestRunner の指定
public class MyModuleTest {
    @Test // テストケースの表明
    public void shouldPass() throws Exception {
        assertTrue(true);
    }
}
