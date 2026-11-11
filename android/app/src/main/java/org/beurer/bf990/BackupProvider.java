package org.beurer.bf990;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/** Read-only provider exposing the freshly written backup file to the share target. */
public final class BackupProvider extends ContentProvider {
    static final String AUTHORITY="org.beurer.bf990.backup";
    static Uri uri(String name){return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(name).build();}
    static File dir(Context c){return new File(c.getCacheDir(),"backups");}
    private File file(Uri uri) throws FileNotFoundException {
        List<String> s=uri.getPathSegments();
        if(s.size()!=1||!s.get(0).matches("bf990-backup-[0-9-]+\\.zip"))throw new FileNotFoundException(uri.toString());
        File f=new File(dir(getContext()),s.get(0));
        if(!f.isFile())throw new FileNotFoundException(uri.toString());
        return f;
    }
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "application/zip";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("read-only");
        return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        File f;try{f=file(uri);}catch(FileNotFoundException e){return null;}
        String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
        Object[] row=new Object[cols.length];
        for(int i=0;i<cols.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))row[i]=f.getName();else if(OpenableColumns.SIZE.equals(cols[i]))row[i]=f.length();}
        MatrixCursor c=new MatrixCursor(cols);c.addRow(row);return c;
    }
    @Override public Uri insert(Uri uri,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
