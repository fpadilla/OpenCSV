package com.opencsv;

import com.opencsv.util.StringUtils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLWriter extends CSVWriter {

    protected String columns;
    protected int initSize;
    private int maxLineWidth;
    private String fileHeader = "";
    static final int SQL_BUFFER_SIZE=8000000;

    public SQLWriter(Writer writer) {
        super(writer, ',', '\'', '\'', "\n");
        extensionName = "sql";
        setBufferSize(SQL_BUFFER_SIZE);
    }

    public SQLWriter(String fileName) throws IOException {
        super(fileName, ',', '\'', '\'', "\n");
        extensionName = "sql";
        setBufferSize(SQL_BUFFER_SIZE);
    }

    public void setCSVDataTypes(ResultSet rs) throws SQLException {
        columnTypes = resultService.getColumnTypes(rs);
        columnNames = resultService.getColumnNames(rs);
    }

    public void setMaxLineWidth(int width) {
        maxLineWidth = width;
    }

    public void writeNextRow(String[] nextLine) throws IOException {
        if (nextLine == null) return;
        if(totalRows==0) writeLog(0);
        add(columns);
        lineWidth = 2;
        for (int i = 0; i < nextLine.length; i++) {
            if (i != 0) add(separator);
            if (lineWidth > maxLineWidth) {
                add(lineEnd).add("    ");
                lineWidth = 4;
            }
            String nextElement = nextLine[i];
            int quotePos = -1;
            Boolean isQuote = columnTypes[i] != "number" && columnTypes[i] != "boolean" && !nextElement.equals("");

            if (isQuote) {
                add(quotechar);
                quotePos = nextElement.lastIndexOf(quotechar);
            }
            if (quotePos != -1) {
                int orgSize = buffeWidth;
                add(nextElement);
                while (quotePos != -1) {
                    sb.insert(orgSize + quotePos, quotechar);
                    ++buffeWidth;
                    quotePos = nextElement.lastIndexOf(quotechar, quotePos - 1);
                }
            } else {
                add(nextElement.equals("") ? "null" : nextElement);
            }
            if (isQuote) add(quotechar);
        }
        add(");").add(lineEnd);
        ++totalRows;
        if (buffeWidth >= INITIAL_BUFFER_SIZE - maxLineWidth) flush();
    }

    private void init(String[] titles, String headerEncloser, int maxLineWidth) {
        columns = headerEncloser + StringUtils.join(titles, headerEncloser + "," + headerEncloser) + headerEncloser;
        columns = "INSERT INTO " + tableName + "(" + columns + ")" + lineEnd + "  VALUES(";
        sb = new StringBuilder(initSize);
        buffeWidth = 0;
        lineWidth = 0;
        this.maxLineWidth = maxLineWidth;
        add(fileHeader==null?"":fileHeader).add(lineEnd);
    }

    public void setFileHead(String header) {
        fileHeader = header;
    }

    public int writeAll2SQL(ResultSet rs) throws SQLException, IOException {
        return writeAll2SQL(rs, "", 9999);
    }

    public int writeAll2SQL(ResultSet rs, String headerEncloser, int maxLineWidth) throws SQLException, IOException {
        init(resultService.getColumnNames(rs), headerEncloser, maxLineWidth);
        columnTypes = resultService.getColumnTypes(rs);
        rs.setFetchSize(RESULT_FETCH_SIZE);
        while (rs.next()) {
            writeNextRow(resultService.getColumnValues(rs, false));
        }
        flush();
        return totalRows;
    }

    public int writeAll2SQL(CSVReader reader) throws IOException {
        return writeAll2SQL(reader, "", 9999);
    }

    public int writeAll2SQL(String CSVFileSource) throws IOException {
        this.CSVFileName = CSVFileSource;
        return writeAll2SQL(new CSVReader(new FileReader(CSVFileSource)));
    }

    public int writeAll2SQL(CSVReader reader, String headerEncloser, int maxLineWidth) throws IOException {
        String[] array = reader.readNext();
        String types[] = new String[array.length];
        for (int i = 0; i < array.length; i++) types[i] = "string";
        if (columnNames != null) {
            for (int i = 0; i < columnNames.length; i++)
                for (int j = 0; j < array.length; j++)
                    if (columnNames[i].equalsIgnoreCase(array[j].trim())) {
                        array[j] = columnNames[i];
                        types[j] = columnTypes[i];
                    }
            columnTypes = types;
            createOracleCtlFileFromHeaders(CSVFileName, array, reader.getParser().getQuotechar());
            columnNames = null;
        }
        columnTypes = types;
        init(array, headerEncloser, maxLineWidth);
        while ((array = reader.readNext()) != null) {
            writeNextRow(array);
        }
        flush();

        return totalRows;
    }
}
