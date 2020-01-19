package trac.svc.meta.dal;

import org.junit.jupiter.api.extension.ExtendWith;
import trac.common.metadata.MetadataCodec;
import trac.common.metadata.ObjectType;
import trac.svc.meta.dal.impls.JdbcH2Impl;
import trac.svc.meta.dal.impls.JdbcMysqlImpl;
import trac.svc.meta.exception.MissingItemError;

import static trac.svc.meta.dal.MetadataDalTestData.*;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import trac.svc.meta.exception.WrongItemTypeError;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;


abstract class MetadataDalReadTest extends MetadataDalTestBase {

    @ExtendWith(JdbcMysqlImpl.class)
    static class JdbcMysql extends MetadataDalReadTest {}

    @ExtendWith(JdbcH2Impl.class)
    static class JdbcH2 extends MetadataDalReadTest {}

    @Test
    void testLoadOneExplicit_ok() throws Exception {

        var origDef = dummyDataDef();
        var origTag = dummyTag(origDef);
        var nextDefTag1 = dummyTag(nextDataDef(origDef));
        var nextDefTag2 = nextTag(nextDefTag1);
        var origId = MetadataCodec.decode(origDef.getHeader().getId());

        // Save v1 t1, v2 t1, v2 t2
        var future = CompletableFuture.completedFuture(0)
                .thenCompose(x -> dal.saveNewObject(TEST_TENANT, origTag))
                .thenCompose(x -> dal.saveNewVersion(TEST_TENANT, nextDefTag1))
                .thenCompose(x -> dal.saveNewTag(TEST_TENANT, nextDefTag2));

        unwrap(future);

        // Load all three items by explicit version / tag number
        var v1t1 = unwrap(dal.loadTag(TEST_TENANT, ObjectType.DATA, origId, 1, 1));
        var v2t1 = unwrap(dal.loadTag(TEST_TENANT, ObjectType.DATA, origId, 2, 1));
        var v2t2 = unwrap(dal.loadTag(TEST_TENANT, ObjectType.DATA, origId, 2, 2));

        assertEquals(origTag, v1t1);
        assertEquals(nextDefTag1, v2t1);
        assertEquals(nextDefTag2, v2t2);
    }

    @Test
    void testLoadOneLatestVersion_ok() throws Exception {

        var origDef = dummyDataDef();
        var origTag = dummyTag(origDef);
        var nextDefTag1 = dummyTag(nextDataDef(origDef));
        var nextDefTag2 = nextTag(nextDefTag1);
        var origId = MetadataCodec.decode(origDef.getHeader().getId());

        // After save v1t1, latest version = v1t1
        var v1t1 = CompletableFuture.completedFuture(0)
                .thenCompose(x -> dal.saveNewObject(TEST_TENANT, origTag))
                .thenCompose(x -> dal.loadLatestVersion(TEST_TENANT, ObjectType.DATA, origId));

        assertEquals(origTag, unwrap(v1t1));

        // After save v2t1, latest version = v2t1
        var v2t1 = CompletableFuture.completedFuture(0)
                .thenCompose(x -> dal.saveNewVersion(TEST_TENANT, nextDefTag1))
                .thenCompose(x -> dal.loadLatestVersion(TEST_TENANT, ObjectType.DATA, origId));

        assertEquals(nextDefTag1, unwrap(v2t1));

        // After save v2t2, latest version = v2t2
        var v2t2 = CompletableFuture.completedFuture(0)
                .thenCompose(x -> dal.saveNewTag(TEST_TENANT, nextDefTag2))
                .thenCompose(x -> dal.loadLatestVersion(TEST_TENANT, ObjectType.DATA, origId));

        assertEquals(nextDefTag2, unwrap(v2t2));
    }

    @Test
    void testLoadOneLatestTag_ok() throws Exception {

        var origDef = dummyDataDef();
        var origTag = dummyTag(origDef);
        var origId = MetadataCodec.decode(origDef.getHeader().getId());

        var nextDefTag1 = dummyTag(nextDataDef(origDef));

        // Save v1 t1, v2 t1
        var future = CompletableFuture.completedFuture(0)
                .thenCompose(x -> dal.saveNewObject(TEST_TENANT, origTag))
                .thenCompose(x -> dal.saveNewVersion(TEST_TENANT, nextDefTag1));

        unwrap(future);

        // Load latest tag for object versions 1 & 2
        var v1 = unwrap(dal.loadLatestTag(TEST_TENANT, ObjectType.DATA, origId, 1));
        var v2 = unwrap(dal.loadLatestTag(TEST_TENANT, ObjectType.DATA, origId, 2));

        // Should get v1 = v1t1, v2 = v2t1
        assertEquals(origTag, v1);
        assertEquals(nextDefTag1, v2);

        // Save a new tag for object version 1
        var origDefTag2 = nextTag(origTag);

        var v1t2 = CompletableFuture.completedFuture(0)
                .thenCompose(x -> dal.saveNewTag(TEST_TENANT, origDefTag2))
                .thenCompose(x -> dal.loadLatestTag(TEST_TENANT, ObjectType.DATA, origId, 1));

        assertEquals(origDefTag2, unwrap(v1t2));
    }

    @Test
    void testLoadOne_missingItems() throws Exception {

        assertThrows(MissingItemError.class, () -> unwrap(dal.loadTag(TEST_TENANT, ObjectType.DATA, UUID.randomUUID(), 1, 1)));
        assertThrows(MissingItemError.class, () -> unwrap(dal.loadLatestTag(TEST_TENANT, ObjectType.DATA, UUID.randomUUID(), 1)));
        assertThrows(MissingItemError.class, () -> unwrap(dal.loadLatestVersion(TEST_TENANT, ObjectType.DATA, UUID.randomUUID())));

        var origDef = dummyDataDef();
        var origTag = dummyTag(origDef);
        var origId = MetadataCodec.decode(origDef.getHeader().getId());

        // Save an item
        var future = dal.saveNewObject(TEST_TENANT, origTag);
        unwrap(future);

        assertThrows(MissingItemError.class, () -> unwrap(dal.loadTag(TEST_TENANT, ObjectType.DATA, origId, 1, 2)));  // Missing tag
        assertThrows(MissingItemError.class, () -> unwrap(dal.loadTag(TEST_TENANT, ObjectType.DATA, origId, 2, 1)));  // Missing ver
        assertThrows(MissingItemError.class, () -> unwrap(dal.loadLatestTag(TEST_TENANT, ObjectType.DATA, origId, 2)));  // Missing ver
    }

    @Test
    void testLoadOne_wrongObjectType() throws Exception {

        var origDef = dummyDataDef();
        var origTag = dummyTag(origDef);
        var origId = MetadataCodec.decode(origDef.getHeader().getId());

        unwrap(dal.saveNewObject(TEST_TENANT, origTag));

        assertThrows(WrongItemTypeError.class, () -> unwrap(dal.loadTag(TEST_TENANT, ObjectType.MODEL, origId, 1, 1)));
        assertThrows(WrongItemTypeError.class, () -> unwrap(dal.loadLatestTag(TEST_TENANT, ObjectType.MODEL, origId, 1)));
        assertThrows(WrongItemTypeError.class, () -> unwrap(dal.loadLatestVersion(TEST_TENANT, ObjectType.MODEL, origId)));
    }

    @Test
    void testLoadBatchExplicit_ok() throws Exception {

        var origDef = dummyDataDef();
        var origTag = dummyTag(origDef);
        var nextDefTag1 = dummyTag(nextDataDef(origDef));
        var nextDefTag2 = nextTag(nextDefTag1);
        var origId = MetadataCodec.decode(origDef.getHeader().getId());

        var modelDef = dummyModelDef();
        var modelTag = dummyTag(modelDef);
        var modelId = MetadataCodec.decode(modelDef.getHeader().getId());

        // Save everything first
        var future = CompletableFuture.completedFuture(0)
                .thenCompose(x -> dal.saveNewObject(TEST_TENANT, origTag))
                .thenCompose(x -> dal.saveNewVersion(TEST_TENANT, nextDefTag1))
                .thenCompose(x -> dal.saveNewTag(TEST_TENANT, nextDefTag2))
                .thenCompose(x -> dal.saveNewObject(TEST_TENANT, modelTag));

        unwrap(future);

        var types = Arrays.asList(ObjectType.DATA, ObjectType.DATA, ObjectType.DATA, ObjectType.MODEL);
        var ids = Arrays.asList(origId, origId, origId, modelId);
        var versions = Arrays.asList(1, 2, 2, 1);
        var tagVersions = Arrays.asList(1, 1, 2, 1);

        var result = unwrap(dal.loadTags(TEST_TENANT, types, ids, versions, tagVersions));

        assertEquals(origTag, result.get(0));
        assertEquals(nextDefTag1, result.get(1));
        assertEquals(nextDefTag2, result.get(2));
        assertEquals(modelTag, result.get(3));
    }

    @Test
    void testLoadBatchLatestVersion_ok() {
        fail("Not implemented");
    }

    @Test
    void testLoadBatchLatestTag_ok() {
        fail("Not implemented");
    }

    @Test
    void testLoadBatch_missingItems() {
        fail("Not implemented");
    }

    @Test
    void testLoadBatch_wrongObjectType() {
        fail("Not implemented");
    }
}