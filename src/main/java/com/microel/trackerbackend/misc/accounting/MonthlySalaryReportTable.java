package com.microel.trackerbackend.misc.accounting;

import com.microel.trackerbackend.services.api.ResponseException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySalaryReportTable implements TDocument{
    private String name;
    private String mimeType;
    private byte[] content;

    @Override
    public long getSize(){
        return content.length;
    }

    @Override
    public void sendByResponse(HttpServletResponse response) {
        try {
            OutputStream os = response.getOutputStream();
            response.setHeader("Content-Type", getMimeType());
            response.setHeader("Content-Length", String.valueOf(getSize()));
            response.setHeader("Content-Disposition", "inline;filename="+getName());

            response.setStatus(HttpStatus.OK.value());

            for (int i = 0; i < getSize(); i++) {
                try {
                    os.write(getContent()[i]);
                } catch (IOException e) {
                    throw new ResponseException("Не удалось записать в буфер ответа");
                }
            }

            os.flush();
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
