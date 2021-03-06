package com.batch.study.job

import com.batch.study.domain.payment.Payment
import com.batch.study.domain.payment.PaymentCsv
import com.batch.study.domain.payment.PaymentCsvMapper
import com.batch.study.listener.JobDataSetUpListener
import com.batch.study.listener.JobReportListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.item.database.JpaPagingItemReader
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import java.nio.charset.StandardCharsets
import javax.persistence.EntityManagerFactory

@Configuration
class CsvWriterJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val jobDataSetUpListener: JobDataSetUpListener,
    entityManagerFactory: EntityManagerFactory
) {
    private val CHUNK_SZIE = 10

    @Bean
    fun csvWriterJob(
        csvWriterStep: Step
    ): Job =
        jobBuilderFactory["csvWriterJob"]
            .incrementer(RunIdIncrementer())
            .listener(JobReportListener())
            .listener(jobDataSetUpListener)
            .start(csvWriterStep)
            .build()

    @Bean
    @JobScope
    fun csvWriterStep(
        stepBuilderFactory: StepBuilderFactory
    ): Step =
        stepBuilderFactory["csvWriterStep"]
            .chunk<Payment, PaymentCsv>(CHUNK_SZIE)
            .reader(reader)
            .writer(writer)
            .build()

    private val reader: JpaPagingItemReader<Payment> =
        JpaPagingItemReaderBuilder<Payment>()
            .queryString("SELECT p FROM Payment p")
            .entityManagerFactory(entityManagerFactory)
            .name("readerPayment")
            .build()

    private val writer: FlatFileItemWriter<PaymentCsv> =
        FlatFileItemWriterBuilder<PaymentCsv>()
            .name("writerPayment")
            .resource(FileSystemResource("src/main/resources/payment.csv"))
            .append(true)
            .lineAggregator(PaymentCsvMapper().delimitedLineAggregator())
            .headerCallback {
                it.write(PaymentCsvMapper().headerNames.joinToString(","))
            }
            .encoding(StandardCharsets.UTF_8.name())
            .build()
}