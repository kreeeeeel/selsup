package com.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@SuppressWarnings("unused")
public class CrptApi {

    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/";

    // Ретрофит - используем для выполнения запросов
    private final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(URL)
            .build();

    private final Api api = retrofit.create(Api.class);

    // Для локов
    private final Object lock = new Objects[0];
    private final LinkedList<Long> lastRequest = new LinkedList<>();

    // Параметры из конструктора
    private final int requestLimit;
    private final long timeInterval;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {

        if (requestLimit < 0) {
            throw new IllegalArgumentException("RequestLimit must be a positive integer");
        }

        this.requestLimit = requestLimit;
        this.timeInterval = timeUnit.toMillis(1);
    }

    public Response<ResponseBody> createRequest(Document document, String signature) throws InterruptedException, IOException {
        synchronized (lock) {
            if (lastRequest.size() >= requestLimit) {

                long firstRequest = lastRequest.getFirst();
                long timeToWait = timeInterval - (System.currentTimeMillis() - firstRequest);

                if (timeToWait > 0) Thread.sleep(timeToWait);
                lastRequest.removeFirst();
            }
            lastRequest.add(System.currentTimeMillis());

        }

        return api.createDocumentForCirculation(signature, document).execute();
    }

    // Дто
    public static class Document {
        @JsonProperty("description") private Description description;
        @JsonProperty("doc_id") private String docId;
        @JsonProperty("doc_status") private String docStatus;
        @JsonProperty("doc_type") private DocumentType docType;
        @JsonProperty("importRequest") private Boolean importRequest;
        @JsonProperty("owner_inn") private String ownerInn;
        @JsonProperty("participant_inn") private String participantInn;
        @JsonProperty("producer_inn") private String producerInn;
        @JsonProperty("production_date") private LocalDate productionDate;
        @JsonProperty("production_type") private String productionType;
        @JsonProperty("products") private List<Product> products;
        @JsonProperty("reg_date") private LocalDate regDate;
        @JsonProperty("reg_number") private String regNumber;
    }

    public enum DocumentType {
        LP_INTRODUCE_GOODS,
    }

    public static class Description {
        @JsonProperty("participantInn") private String participantInn;
    }

    public static class Product {
        @JsonProperty("certificate_document") private String certificateDocument;
        @JsonProperty("certificate_document_date") private LocalDate certificateDocumentDate;
        @JsonProperty("certificate_document_number") private String certificateDocumentNumber;
        @JsonProperty("owner_inn") private String ownerInn;
        @JsonProperty("producer_inn") private String producerInn;
        @JsonProperty("production_date") private LocalDate productionDate;
        @JsonProperty("tnved_code") private String tnvedCode;
        @JsonProperty("uit_code") private String uitCode;
        @JsonProperty("uitu_code") private String uituCode;
    }

    // Интерфейс для работы с ретрофит
    interface Api {

        @POST("create")
        Call<ResponseBody> createDocumentForCirculation(
                @Header("Signature") String signature,
                @Body Document document
        );

    }
}
