package org.tron.storage.leveldb;

import org.junit.Ignore;
import org.junit.Test;
import org.tron.config.Configer;
import org.tron.core.Constant;
import org.tron.utils.ByteArray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore
public class LevelDbDataSourceImplTest {

    @Test
    public void testGet()  {
        Configer.TRON_CONF= Constant.TEST_CONF;
        LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl("test");
        dataSource.initDB();
        String key1="000134yyyhy";
        byte[] key = key1.getBytes();
        byte[] value = dataSource.getData(key);
        String s = ByteArray.toStr(value);
        dataSource.closeDB();
        System.out.println(s);
    }

    @Test
    public void testPut() {
        Configer.TRON_CONF= Constant.TEST_CONF;
        LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl("test");
        dataSource.initDB();
        String key1="000134yyyhy";
        byte[] key = key1.getBytes();

        String value1="50000";
        byte[] value = value1.getBytes();

        dataSource.putData(key,value);

        assertNotNull(dataSource.getData(key));
        assertEquals(1, dataSource.allKeys().size());

        dataSource.closeDB();
    }

    @Test
    public void testRest() {
        Configer.TRON_CONF= Constant.TEST_CONF;
        LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl("test");
        dataSource.resetDB();
        dataSource.closeDB();
    }

}