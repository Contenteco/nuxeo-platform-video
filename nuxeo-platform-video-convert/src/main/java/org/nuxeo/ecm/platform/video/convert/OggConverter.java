/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.convert;

/**
 * Convert to Ogg format.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 * @deprecated since 5.9.5. Use {@link org.nuxeo.ecm.platform.video.convert.VideoConversionConverter}.
 */
@Deprecated
public class OggConverter extends BaseVideoConversionConverter {

    public static final String OGG_VIDEO_MIMETYPE = "video/ogg";

    public static final String OGG_EXTENSION = ".ogg";

    public static final String TMP_DIRECTORY_PREFIX = "convertToOgg";

    @Override
    protected String getVideoMimeType() {
        return OGG_VIDEO_MIMETYPE;
    }

    @Override
    protected String getVideoExtension() {
        return OGG_EXTENSION;
    }

    @Override
    protected String getTmpDirectoryPrefix() {
        return TMP_DIRECTORY_PREFIX;
    }

}
