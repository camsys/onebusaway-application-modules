package org.onebusaway.transit_data_federation.impl.realtime.gtfs_tripmodifications;

import com.google.protobuf.util.JsonFormat;
import com.google.transit.realtime.GtfsRealtime.*;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


public class FeedEntitySortingTest {

    private static final String ID_4926 = "4926a464-16b6-4259-a3cb-258a610f2a34";
    private static final String ID_638F = "638fbfd7-ad3f-4f14-8b97-64569e508141";
    private static final String ID_46C7 = "46c73d9a-ea65-4f5e-8728-4af279a6e0d8";
    private static final String ID_4F04 = "4f047e85-75be-41cc-bd42-dcc1817e4595";
    private static final String ID_16D9 = "16d97543-16ad-472a-ac9d-a5c833441c4c";
    private static final String ID_847B = "847b8342-e742-4f92-8b00-f58ef5f439bd";

    private FeedMessage feedMessage;

    @Before
    public void setUp() throws Exception {
        Path jsonPath = Paths.get(getClass().getResource("impl/trip-modifications.json").toURI());
        String json = new String(Files.readAllBytes(jsonPath));
        FeedMessage.Builder builder = FeedMessage.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        feedMessage = builder.build();
    }

    private List<FeedEntity> sort(List<FeedEntity> entities) {
        return entities.stream()
                .sorted(Comparator.comparing(FeedEntity::getId)
                        .thenComparingInt(GtfsTripModificationsClientImpl::getEntityType))
                .collect(Collectors.toList());
    }

    private byte[] computeHash(List<FeedEntity> entities) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (FeedEntity entity : entities) {
            md.update(entity.toByteArray());
        }
        return md.digest();
    }

    // --- Sorting tests ---

    @Test
    public void testIdsAreSortedAlphabetically() {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());
        List<String> ids = sorted.stream().map(FeedEntity::getId).collect(Collectors.toList());

        assertTrue(ids.indexOf(ID_16D9) < ids.indexOf(ID_46C7));
        assertTrue(ids.indexOf(ID_46C7) < ids.indexOf(ID_4926));
        assertTrue(ids.indexOf(ID_4926) < ids.indexOf(ID_4F04));
        assertTrue(ids.indexOf(ID_4F04) < ids.indexOf(ID_638F));
        assertTrue(ids.indexOf(ID_638F) < ids.indexOf(ID_847B));
    }

    @Test
    public void testAlertAlwaysFirstWithinSameId() {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());

        for (String id : List.of(ID_4926, ID_638F, ID_46C7, ID_4F04, ID_16D9, ID_847B)) {
            List<FeedEntity> group = sorted.stream()
                    .filter(e -> e.getId().equals(id))
                    .collect(Collectors.toList());

            assertTrue("First entity for id " + id + " should be alert", group.get(0).hasAlert());
        }
    }

    @Test
    public void testTripModificationsAlwaysLastWithinSameId() {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());

        for (String id : List.of(ID_4926, ID_638F, ID_46C7, ID_4F04, ID_16D9, ID_847B)) {
            List<FeedEntity> group = sorted.stream()
                    .filter(e -> e.getId().equals(id))
                    .collect(Collectors.toList());

            assertTrue("Last entity for id " + id + " should be tripModifications", group.get(group.size() - 1).hasTripModifications());
        }
    }

    @Test
    public void testShapesBetweenAlertAndTripModificationsForMultiShapeGroups() {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());

        // 4926, 638f, 4f04 each have 2 shapes and 2 tripMods: alert, shape, shape, tripMod, tripMod
        for (String id : List.of(ID_4926, ID_638F, ID_4F04)) {
            List<FeedEntity> group = sorted.stream()
                    .filter(e -> e.getId().equals(id))
                    .collect(Collectors.toList());

            assertEquals("Expected 5 entities for id " + id, 5, group.size());
            assertTrue(group.get(0).hasAlert());
            assertTrue(group.get(1).hasShape());
            assertTrue(group.get(2).hasShape());
            assertTrue(group.get(3).hasTripModifications());
            assertTrue(group.get(4).hasTripModifications());
        }
    }

    @Test
    public void testSingleShapeGroupOrder() {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());

        // 46c7, 16d9, 847b each have 1 shape: alert, shape, tripMod
        for (String id : List.of(ID_46C7, ID_16D9, ID_847B)) {
            List<FeedEntity> group = sorted.stream()
                    .filter(e -> e.getId().equals(id))
                    .collect(Collectors.toList());

            assertEquals("Expected 3 entities for id " + id, 3, group.size());
            assertTrue(group.get(0).hasAlert());
            assertTrue(group.get(1).hasShape());
            assertTrue(group.get(2).hasTripModifications());
        }
    }

    @Test
    public void testTotalEntityCountPreserved() {
        List<FeedEntity> original = feedMessage.getEntityList();
        assertEquals(original.size(), sort(original).size());
    }

    @Test
    public void testSortIsDeterministic() {
        List<FeedEntity> entities = feedMessage.getEntityList();

        List<String> first = sort(entities).stream()
                .map(e -> e.getId() + ":" + GtfsTripModificationsClientImpl.getEntityType(e))
                .collect(Collectors.toList());
        List<String> second = sort(entities).stream()
                .map(e -> e.getId() + ":" + GtfsTripModificationsClientImpl.getEntityType(e))
                .collect(Collectors.toList());

        assertEquals(first, second);
    }

    // --- Hash tests ---

    @Test
    public void testHashIsConsistentForSameFeed() throws Exception {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());
        List<FeedEntity> sorted2 = sort(feedMessage.getEntityList());

        byte[] hash1 = computeHash(sorted);
        byte[] hash2 = computeHash(sorted2);

        assertArrayEquals(hash1, hash2);
    }

    @Test
    public void testHashChangesWhenEntityIsRemoved() throws Exception {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());
        List<FeedEntity> modified = sorted.subList(0, sorted.size() - 1); // drop last entity

        byte[] originalHash = computeHash(sorted);
        byte[] modifiedHash = computeHash(modified);

        assertFalse("Hash should change when an entity is removed",
                Arrays.equals(originalHash, modifiedHash));
    }

    @Test
    public void testHashChangesWhenEntityIsAdded() throws Exception {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());

        FeedEntity extraEntity = FeedEntity.newBuilder()
                .setId("ffffffff-ffff-ffff-ffff-ffffffffffff")
                .setAlert(Alert.newBuilder())
                .build();

        List<FeedEntity> modified = new java.util.ArrayList<>(sorted);
        modified.add(extraEntity);

        byte[] originalHash = computeHash(sorted);
        byte[] modifiedHash = computeHash(modified);

        assertFalse("Hash should change when an entity is added",
                Arrays.equals(originalHash, modifiedHash));
    }

    @Test
    public void testHashChangesWhenEntityContentChanges() throws Exception {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());

        // Replace the first entity with the same id but different alert text
        FeedEntity original = sorted.get(0);
        FeedEntity altered = FeedEntity.newBuilder(original)
                .setAlert(Alert.newBuilder()
                        .setHeaderText(TranslatedString.newBuilder()
                                .addTranslation(TranslatedString.Translation.newBuilder()
                                        .setText("modified alert text"))))
                .build();

        List<FeedEntity> modified = new java.util.ArrayList<>(sorted);
        modified.set(0, altered);

        byte[] originalHash = computeHash(sorted);
        byte[] modifiedHash = computeHash(modified);

        assertFalse("Hash should change when entity content changes",
                Arrays.equals(originalHash, modifiedHash));
    }

    @Test
    public void testUnsortedFeedProducesDifferentHashThanSorted() throws Exception {
        List<FeedEntity> sorted = sort(feedMessage.getEntityList());
        assertTrue("Test fixture must contain at least two entities to verify order-dependent hashing",
                sorted.size() > 1);

        List<FeedEntity> unsorted = new java.util.ArrayList<>(sorted);
        java.util.Collections.reverse(unsorted);

        assertFalse("Test setup must produce a different entity order than the sorted list",
                unsorted.equals(sorted));

        byte[] unsortedHash = computeHash(unsorted);
        byte[] sortedHash = computeHash(sorted);

        assertFalse("Sorted and unsorted feeds should produce different hashes",
                Arrays.equals(unsortedHash, sortedHash));
    }
}