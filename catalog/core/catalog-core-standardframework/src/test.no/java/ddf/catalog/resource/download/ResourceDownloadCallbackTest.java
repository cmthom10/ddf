/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.resource.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.cache.MockInputStream;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.cache.impl.ResourceCache;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.event.retrievestatus.DownloadStatusInfo;
import ddf.catalog.event.retrievestatus.DownloadStatusInfoImpl;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventListener;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.data.ReliableResource;
import ddf.catalog.resourceretriever.ResourceRetriever;


public class ResourceDownloadCallbackTest
{
    private ReliableResourceDownloader downloader;
    private FileOutputStream fos;
    private ReliableResource reliableResource;
    private ResourceCache resourceCache;
    private ReliableResourceStatus reliableResourceStatus;

    @Before
    public void setup()
    {
        downloader = mock(ReliableResourceDownloader.class);
        fos = mock(FileOutputStream.class);
        reliableResource = mock(ReliableResource.class);
        resourceCache = mock(ResourceCache.class);
        reliableResourceStatus = mock(ReliableResourceStatus.class);
    }

    @Test
    public void onSuccessStatusComplete() throws Exception
    {

        when(reliableResourceStatus.getBytesRead()).thenReturn(100L);
        when(downloader.getReliableResourceStatus()).thenReturn(reliableResourceStatus);

        when(downloader.getDownloadStatus()).thenReturn(DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE);

        when(reliableResource.getKey()).thenReturn("something");

        ResourceDownloadCallback resourceDownloadCallback = new ResourceDownloadCallback(downloader, fos, reliableResource, resourceCache);

        resourceDownloadCallback.onSuccess(null);
        verify(reliableResource, times(1)).setSize(reliableResourceStatus.getBytesRead());
        verify(resourceCache, times(1)).put(reliableResource);
        verify(resourceCache, times(1)).removePendingCacheEntry(reliableResource.getKey());

    }

    @Test
    public void onSuccessNotComplete() throws Exception
    {
        when(downloader.getReliableResourceByteSize()).thenReturn(100L);

        when(reliableResourceStatus.getBytesRead()).thenReturn(100L);
        when(downloader.getReliableResourceStatus()).thenReturn(reliableResourceStatus);
        when(reliableResource.getKey()).thenReturn("something");

        when(downloader.getDownloadStatus()).thenReturn(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED);

        ResourceDownloadCallback resourceDownloadCallback = new ResourceDownloadCallback(downloader, fos, reliableResource, resourceCache);

        resourceDownloadCallback.onSuccess(null);
        verify(downloader, times(1)).closeFOS();
        verify(resourceCache, times(1)).removePendingCacheEntry(reliableResource.getKey());
    }
}