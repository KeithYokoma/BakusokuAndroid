import android.test.AndroidTestCase;

import jp.yokomark.sample.MyModule;

public class MyModuleTest extends AndroidTestCase {
    private MyModule mTarget;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTarget = new MyModule();
    }

    public void testPlus() throws Exception {
        assertEquals(2, mTarget.plus(1, 1));
    }
}