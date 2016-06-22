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
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cxf.common.i18n.Exception;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.CountingOutputStream;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.resource.data.ReliableResource;


public class ReliableResourceCallableTest {

    private AtomicLong bytesRead;

    private Object lock;

    private InputStream input = null;

    private CountingOutputStream countingFbos;

    private FileOutputStream cacheFileOutputStream = null;

    private File cacheFile;

    private int chunkSize;

    private ReliableResourceStatus reliableResourceStatus;


    @Before
    public void setup()
    {
        bytesRead = mock(AtomicLong.class);
        lock = mock(Object.class);
        input = mock(InputStream.class);
        countingFbos = null;
        cacheFileOutputStream = mock(FileOutputStream.class);
        cacheFile = mock(File.class);
        chunkSize = 0;
        reliableResourceStatus = mock(ReliableResourceStatus.class);
    }


    @Test
    public void getBytesReadTest()
    {


    }

    @Test
    public void getReliableResourceStatusIsNull()
    {

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
                chunkSize, lock);

       // assertThat(testReliableResourceCallable.getReliableResourceStatus(), isNull());
    }

    @Test
    public void setInterruptDownloadToTrue()
    {


        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
        chunkSize, lock);

        testReliableResourceCallable.setInterruptDownload(true);
//        verify(bytesRead, times(1)).get();
        verify(reliableResourceStatus, times(1)).setMessage(
                "Download interrupted - returning " + bytesRead + " bytes read");
    }

    @Test
    public void setCancelDownloadToTrue()
    {

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
                chunkSize, lock);

        testReliableResourceCallable.setCancelDownload(true);
//        verify(bytesRead, times(1)).get();
        verify(reliableResourceStatus, times(1)).setMessage(
                "Download canceled - returning " + bytesRead + " bytes read");

    }

//    @Test
//    public void closeAndDeleteCacheFileTest() throws Exception, IOException {
//
//
//        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
//                chunkSize, lock);
//
//        verify(cacheFileOutputStream, times(1)).close();
//    }

}
