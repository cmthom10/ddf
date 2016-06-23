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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyChar;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cxf.common.i18n.Exception;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.io.CountingOutputStream;



public class ReliableResourceCallableTest {

    private static final int END_OF_FILE = -1;

    private AtomicLong bytesRead;

    private Object lock;

    private InputStream input = null;

    private ByteArrayOutputStream streamArray = new ByteArrayOutputStream();
    private CountingOutputStream countingFbos = new CountingOutputStream(streamArray);

    private FileOutputStream cacheFileOutputStream = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private File cacheFile;

    public File testFile;

    private int chunkSize;

    private ReliableResourceStatus reliableResourceStatus;


    @Before
    public void setup() throws IOException {
        bytesRead = new AtomicLong(0);
        lock = mock(Object.class);
        input = mock(InputStream.class);
        //countingFbos = null;
        cacheFileOutputStream = mock(FileOutputStream.class);
        cacheFile = mock(File.class);
        testFile = testFolder.newFile("test.txt");
        chunkSize = 0;
        reliableResourceStatus = mock(ReliableResourceStatus.class);
    }

    @Test
    /**
     * Checks that the value returned by getBytesRead changes to a new value after setBytesRead
     * has been passed a new value.
     */
    public void setBytesReadChangeValue() throws Exception
    {

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
                chunkSize, lock);

        testReliableResourceCallable.setBytesRead(0L);
        assertThat(testReliableResourceCallable.getBytesRead(), is(0L));
        testReliableResourceCallable.setBytesRead(15L);
        assertThat(testReliableResourceCallable.getBytesRead(), is(15L));
    }

    @Test
    /**
     * Checks that when passed a true value, the set interrupt download recreates the reliable
     * resource status with the new interrupted status and the number of bytes in bytesRead.
     * Determines if the bytesRead are correct by comparing with what is returned by the
     * reliable resource status's getBytesRead method.
     */
    public void setInterruptDownloadToTrue() throws Exception
    {

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
        chunkSize, lock);

        testReliableResourceCallable.setInterruptDownload(true);

        assertThat(testReliableResourceCallable.getReliableResourceStatus().getDownloadStatus(), is(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED));
        assertThat(testReliableResourceCallable.getReliableResourceStatus().getBytesRead(), is(0L));
    }

    @Test
    /**
     * Checks that when passed a true value, the set cancel download recreates the reliable
     * resource status with the new cancelled status and the number of bytes in bytesRead.
     * Determines if the bytesRead are correct by comparing with what is returned by the
     * reliable resource status's getBytesRead method.
     */
    public void setCancelDownloadToTrue() throws Exception
    {

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
                chunkSize, lock);

        testReliableResourceCallable.setCancelDownload(true);

        assertThat(testReliableResourceCallable.getReliableResourceStatus().getDownloadStatus(), is(DownloadStatus.RESOURCE_DOWNLOAD_CANCELED));
        assertThat(testReliableResourceCallable.getReliableResourceStatus().getBytesRead(), is(0L));

    }

    @Test
    /**
     *
     */
    public void callDownloadSuccess() throws Exception, IOException
    {
        ReliableResourceStatus returnedStatus = null;

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
                testFile, chunkSize, lock);

        when(input.read(any(byte[].class))).thenReturn(END_OF_FILE);
        returnedStatus =  testReliableResourceCallable.call();

        assertThat(returnedStatus.getDownloadStatus(), is(DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE));

        assertThat(returnedStatus.toString(), is ("bytesRead = 0,  downloadStatus = RESOURCE_DOWNLOAD_COMPLETE,  message = Download completed successfully"));



    }

    @Test
    /**
     * Throw an IOException while reading the input and check that the download status
     * is changed to indicate the product input stream exception by checking the
     * downloadStatus's getDownloadStatus() and toString().
     */
    public void callDownloadIOException() throws Exception, IOException
    {
        ReliableResourceStatus returnedStatus = null;

        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
                testFile, chunkSize, lock);

            when(input.read(any(byte[].class))).thenThrow(new IOException());
            returnedStatus = testReliableResourceCallable.call();

            assertThat(returnedStatus.getDownloadStatus(), is(DownloadStatus.PRODUCT_INPUT_STREAM_EXCEPTION));

            assertThat(returnedStatus.toString(), is("bytesRead = 0,  downloadStatus = PRODUCT_INPUT_STREAM_EXCEPTION,  message = IOException during read of product's InputStream"));

    }

    @Test
    public void callDownloadWritesToClientOutputStream() throws IOException
    {
        int n = 5;
        chunkSize = 5;
        byte[] expectedBuffer = new byte[5];
        Arrays.fill(expectedBuffer, (byte) 5);

       // ArgumentCaptor<byte[]> buffer = ArgumentCaptor.forClass(byte[].class);
        when(input.read(any(byte[].class))).thenAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                byte[] buffer = (byte[])(args[0]);
                Arrays.fill(buffer, (byte) 5);
                return n;
            }
        }).thenReturn(END_OF_FILE);

        //countingFbos =
        ReliableResourceCallable testReliableResourceCallable = new ReliableResourceCallable(input, countingFbos,
                testFile, chunkSize, lock);

//        verify(countingFbos).write(any(byte[].class),0,n);
//
//        if(streamArray.toByteArray().length == 5)
//        {
//        }

        testReliableResourceCallable.call();

        assertThat(streamArray.toByteArray(), is(expectedBuffer));
    }


}
