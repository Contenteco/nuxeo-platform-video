/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.video.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.video.VideoConstants.DURATION_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.video.Stream;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * Tests that the VideoImporter class works by importing a sample video
 */
public class TestVideoImporterAndListeners extends SQLRepositoryTestCase {

    // http://www.elephantsdream.org/
    public static final String ELEPHANTS_DREAM = "elephantsdream-160-mpeg4-su-ac3.avi";

    public static final Log log = LogFactory.getLog(TestVideoImporterAndListeners.class);

    protected static final String VIDEO_TYPE = "Video";

    protected FileManager fileManagerService;

    protected DocumentModel root;

    private File getTestFile() {
        return new File(FileUtils.getResourcePathFromContext("test-data/sample.mpg"));
    }

    protected static BlobHolder getBlobFromPath(String path) throws IOException {
        Blob blob;
        try (InputStream in = TestVideoImporterAndListeners.class.getResourceAsStream("/" + path)) {
            assertNotNull(String.format("Failed to load resource: " + path), in);
            blob = new FileBlob(in);
        }
        blob.setFilename(path);
        return new SimpleBlobHolder(blob);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.convert");
        deployBundle("org.nuxeo.ecm.platform.video.convert");
        deployBundle("org.nuxeo.ecm.platform.video.core");

        // use these to get the fileManagerService
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");

        openSession();

        EventServiceAdmin eventServiceAdmin = Framework.getLocalService(EventServiceAdmin.class);
        eventServiceAdmin.setListenerEnabledFlag("videoAutomaticConversions", false);
        eventServiceAdmin.setListenerEnabledFlag("sql-storage-binary-text", false);

        root = session.getRootDocument();
        fileManagerService = Framework.getService(FileManager.class);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        fileManagerService = null;
        root = null;
        closeSession();
        super.tearDown();
    }

    @Test
    public void testVideoType() throws ClientException {
        DocumentType videoType = session.getDocumentType(VIDEO_TYPE);
        assertNotNull("Does our type exist?", videoType);

        // TODO: check get/set properties on common properties of videos

        // Create a new DocumentModel of our type in memory
        DocumentModel docModel = session.createDocumentModel("/", "doc", VIDEO_TYPE);
        assertNotNull(docModel);

        assertNull(docModel.getPropertyValue("common:icon"));
        assertNull(docModel.getPropertyValue("dc:title"));
        assertNull(docModel.getPropertyValue("picture:credit"));
        assertNull(docModel.getPropertyValue("uid:uid"));
        assertNull(docModel.getPropertyValue(DURATION_PROPERTY));

        docModel.setPropertyValue("common:icon", "/icons/video.png");
        docModel.setPropertyValue("dc:title", "testTitle");
        docModel.setPropertyValue("picture:credit", "testUser");
        docModel.setPropertyValue("uid:uid", "testUid");

        DocumentModel docModelResult = session.createDocument(docModel);
        assertNotNull(docModelResult);

        assertEquals("/icons/video.png", docModelResult.getPropertyValue("common:icon"));
        assertEquals("testTitle", docModelResult.getPropertyValue("dc:title"));
        assertEquals("testUser", docModelResult.getPropertyValue("picture:credit"));
        assertEquals("testUid", docModelResult.getPropertyValue("uid:uid"));
        assertEquals("0.0", docModelResult.getPropertyValue(DURATION_PROPERTY).toString());
    }

    @Test
    public void testImportSmallVideo() throws Exception {
        File testFile = getTestFile();
        Blob blob = new FileBlob(testFile, "video/mpg");
        blob.setFilename("Test file.mov");
        String rootPath = root.getPathAsString();
        assertNotNull(blob);
        assertNotNull(rootPath);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        DocumentModel docModel = fileManagerService.createDocumentFromBlob(session, blob, rootPath, true,
                "test-data/sample.mpg");

        assertNotNull(docModel);
        DocumentRef ref = docModel.getRef();
        session.save();

        closeSession();
        openSession();

        docModel = session.getDocument(ref);
        assertEquals("Video", docModel.getType());
        assertEquals("sample.mpg", docModel.getTitle());

        assertNotNull(docModel.getProperty("file:content"));
        assertEquals("sample.mpg", docModel.getPropertyValue("file:filename"));

        CommandAvailability ca = Framework.getService(CommandLineExecutorService.class).getCommandAvailability(
                "ffmpeg-screenshot");
        if (!ca.isAvailable()) {
            log.warn("ffmpeg-screenshot is not avalaible, skipping the end of the test");
            throw new AssumptionViolatedException("ffmpeg-screenshot is not avalaible");
        }

        Framework.getService(EventService.class).waitForAsyncCompletion();

        // the test video is very short, no storyboard:
        Serializable duration = docModel.getPropertyValue(DURATION_PROPERTY);
        if (!Double.valueOf(0.05).equals(duration)) { // ffmpeg 2.2.1
            assertEquals(0.04, duration);
        }
        List<Map<String, Serializable>> storyboard = docModel.getProperty("vid:storyboard").getValue(List.class);
        assertNotNull(storyboard);
        assertEquals(0, storyboard.size());

    }

    @Test
    public void testImportBigVideo() throws Exception {
        CommandAvailability ca = Framework.getService(CommandLineExecutorService.class).getCommandAvailability(
                "ffmpeg-screenshot");
        if (!ca.isAvailable()) {
            log.warn("ffmpeg-screenshot is not avalaible, skipping the end of the test");
            throw new AssumptionViolatedException("ffmpeg-screenshot is not avalaible");
        }
        DocumentModel docModel = session.createDocumentModel("/", "doc", VIDEO_TYPE);
        assertNotNull(docModel);
        docModel.setPropertyValue("file:content", (Serializable) getBlobFromPath(ELEPHANTS_DREAM).getBlob());
        docModel = session.createDocument(docModel);
        session.save();

        Framework.getService(EventService.class).waitForAsyncCompletion();
        session.save();

        docModel = session.getDocument(docModel.getRef());
        // the test video last around 10 minutes
        Serializable duration = docModel.getPropertyValue(DURATION_PROPERTY);
        if (!Double.valueOf(653.81).equals(duration)) { // ffmpeg 2.2.1
            assertEquals(653.8, duration);
        }
        List<Map<String, Serializable>> storyboard = docModel.getProperty("vid:storyboard").getValue(List.class);
        assertNotNull(storyboard);
        assertEquals(9, storyboard.size());

        assertEquals(0.0, storyboard.get(0).get("timecode"));
        assertEquals("elephantsdream-160-mpeg4-su-ac3.avi 0", storyboard.get(0).get("comment"));
        Blob thumb0 = (Blob) storyboard.get(0).get("content");
        assertEquals("00000.000-seconds.jpeg", thumb0.getFilename());

        assertEquals(72.0, storyboard.get(1).get("timecode"));
        assertEquals("elephantsdream-160-mpeg4-su-ac3.avi 1", storyboard.get(1).get("comment"));
        Blob thumb1 = (Blob) storyboard.get(1).get("content");
        assertEquals("00072.000-seconds.jpeg", thumb1.getFilename());

        // check that the thumbnails where extracted
        assertEquals("Small", docModel.getPropertyValue("picture:views/0/title"));
        assertEquals(100L, docModel.getPropertyValue("picture:views/0/height"));
        assertEquals(160L, docModel.getPropertyValue("picture:views/0/width"));
        assertTrue((Long) docModel.getPropertyValue("picture:views/0/content/length") > 1000);

        // the original video is also 100 pixels high hence the player preview
        // has the same size
        assertEquals("StaticPlayerView", docModel.getPropertyValue("picture:views/1/title"));
        assertEquals(100L, docModel.getPropertyValue("picture:views/1/height"));
        assertEquals(160L, docModel.getPropertyValue("picture:views/1/width"));
        assertTrue((Long) docModel.getPropertyValue("picture:views/1/content/length") > 1000);

        // TODO: add picture metadata extraction where if
        // they make sense for videos (ie. extract these from the
        // metadata already included in the video and use them to
        // set the appropriate schema properties)
        assertNull("", docModel.getPropertyValue("picture:credit"));

        // check that the update with a null video removes the previews and
        // storyboard
        docModel.setPropertyValue("file:content", null);
        docModel = session.saveDocument(docModel);

        session.save();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        session.save();

        docModel = session.getDocument(docModel.getRef());

        assertTrue(docModel.getProperty("vid:storyboard").getValue(List.class).isEmpty());
        assertTrue(docModel.getProperty("picture:views").getValue(List.class).isEmpty());
    }

    @Test
    public void testVideoInfo() throws Exception {
        File testFile = getTestFile();
        Blob blob = new FileBlob(testFile, "video/mpg");
        blob.setFilename("Sample.mpg");
        String rootPath = root.getPathAsString();
        assertNotNull(blob);
        assertNotNull(rootPath);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        DocumentModel docModel = fileManagerService.createDocumentFromBlob(session, blob, rootPath, true,
                "test-data/sample.mpg");
        session.save();

        closeSession();
        openSession();

        docModel = session.getDocument(docModel.getRef());
        assertEquals("Video", docModel.getType());
        assertEquals("sample.mpg", docModel.getTitle());

        VideoDocument videoDocument = docModel.getAdapter(VideoDocument.class);
        assertNotNull(videoDocument);

        Video video = videoDocument.getVideo();
        assertNotNull(video);
        assertEquals("mpegvideo", video.getFormat());
        assertEquals(0.04, video.getDuration(), 0.1);
        assertEquals(23.98, video.getFrameRate(), 0.1);
        assertEquals(320, video.getWidth());
        assertEquals(200, video.getHeight());

        List<Stream> streams = video.getStreams();
        assertNotNull(streams);
        assertEquals(1, streams.size());
        Stream stream = streams.get(0);
        assertEquals(Stream.VIDEO_TYPE, stream.getType());
        assertEquals("mpeg1video", stream.getCodec());
        assertEquals(104857, stream.getBitRate(), 0.1);
        String streamInfo = stream.getStreamInfo();
        // assert that the stream info contains common info, to avoid strict
        // equals
        assertTrue(streamInfo.contains("Video: mpeg1video"));
        assertTrue(streamInfo.contains("320x200"));
        assertTrue(streamInfo.contains("23.98 fps"));
    }

}
