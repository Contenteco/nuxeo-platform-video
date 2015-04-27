/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.video.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.video.VideoConstants.VIDEO_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.actions", "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.core.mimetype", "org.nuxeo.ecm.platform.picture.core", "org.nuxeo.ecm.platform.picture.convert",
        "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.platform.rendition.api",
        "org.nuxeo.ecm.platform.rendition.core", "org.nuxeo.ecm.platform.video.convert",
        "org.nuxeo.ecm.platform.video.core" })
public class TestVideoRenditions {

    public static final String DELTA_MP4 = "DELTA.mp4";

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    @Inject
    protected CoreSession session;

    @Inject
    protected VideoService videoService;

    @Inject
    protected RenditionService renditionService;

    @Before
    public void setUp() {
        eventServiceAdmin.setListenerEnabledFlag("videoAutomaticConversions", false);
    }

    @Test
    public void shouldExposeTranscodedVideosAsRenditions() throws IOException {
        Video video = getTestVideo();
        DocumentModel doc = session.createDocumentModel("/", "video", VIDEO_TYPE);
        doc = session.createDocument(doc);

        List<RenditionDefinition> availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(3, availableRenditionDefinitions.size());

        TranscodedVideo transcodedVideo = videoService.convert(video, "WebM 480p");
        assertNotNull(transcodedVideo);
        List<Map<String, Serializable>> transcodedVideos = new ArrayList<>();
        transcodedVideos.add(transcodedVideo.toMap());
        doc.setPropertyValue(VideoConstants.TRANSCODED_VIDEOS_PROPERTY, (Serializable) transcodedVideos);
        doc = session.saveDocument(doc);
        availableRenditionDefinitions = renditionService.getAvailableRenditionDefinitions(doc);
        assertEquals(4, availableRenditionDefinitions.size());
        for (RenditionDefinition definition : availableRenditionDefinitions) {
            if (definition.getName().equals(transcodedVideo.getName())) {
                assertTrue(definition.isEnabled());
                assertTrue(definition.isVisible());
                assertEquals(transcodedVideo.getName(), definition.getName());
                assertEquals(transcodedVideo.getName(), definition.getLabel());
            }
        }

        List<Rendition> availableRenditions = renditionService.getAvailableRenditions(doc, true);
        assertEquals(4, availableRenditions.size());
        availableRenditions = renditionService.getAvailableRenditions(doc, false);
        assertEquals(4, availableRenditions.size());
        for (Rendition rendition : availableRenditions) {
            if (rendition.getName().equals("WebM 480p")) {
                List<Blob> blobs = rendition.getBlobs();
                assertEquals(1, blobs.size());
                Blob blob = blobs.get(0);
                assertEquals(transcodedVideo.getBlob(), blob);
            }
        }
    }

    protected static Video getTestVideo() throws IOException {
        try (InputStream is = TestVideoService.class.getResourceAsStream("/" + DELTA_MP4)) {
            assertNotNull(String.format("Failed to load resource: " + DELTA_MP4), is);
            Blob blob = Blobs.createBlob(is, "video/mp4");
            blob.setFilename(FilenameUtils.getName(DELTA_MP4));
            VideoInfo videoInfo = VideoInfo.fromFFmpegOutput(getTestVideoInfoOutput());
            return Video.fromBlobAndInfo(blob, videoInfo);
        }
    }

    protected static List<String> getTestVideoInfoOutput() {
        List<String> output = new ArrayList<String>();
        output.add("Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'DELTA.mp4':");
        output.add("Duration: 00:00:08.38, start: 0.000000, bitrate: 930 kb/s");
        output.add("Stream #0.0(und): Video: h264 (High), yuv420p, 768x480 [PAR 1:1 DAR 8:5], 927 kb/s, 23.98 fps, 23.98 tbr, 10k tbn, 47.96 tbc");
        return output;
    }
}
