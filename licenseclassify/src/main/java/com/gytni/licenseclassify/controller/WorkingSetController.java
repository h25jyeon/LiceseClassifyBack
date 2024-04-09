package com.gytni.licenseclassify.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gytni.licenseclassify.model.CSVUploadPattern;
import com.gytni.licenseclassify.model.WorkingSet;
import com.gytni.licenseclassify.repo.WorkingSetRepo;
import com.gytni.licenseclassify.service.ProductPatternService;
import com.gytni.licenseclassify.service.WorkingSetService;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/working-set")    
public class WorkingSetController {
    @Autowired
    private WorkingSetRepo workingSetRepo;
    
    @Autowired
    private WorkingSetService workingSetService;

    @Autowired
    private ProductPatternService productPatternService;

    @GetMapping("")
    private ResponseEntity<List<WorkingSet>> getMethodName() {
        Iterable<WorkingSet> wsIter = workingSetRepo.findAll();
        List<WorkingSet> wss = new ArrayList<>();
        wsIter.forEach(wss::add);
        
        if (wss.isEmpty())
            return ResponseEntity.noContent().build();
        else{
            for (WorkingSet ws : wss) 
                ws.setName( (int)(workingSetService.getClassifyPerc(ws.getId()) * 100) + "%\t" + ws.getName());
            return ResponseEntity.ok(wss);
        }
    }

    @PostMapping("")
    private ResponseEntity<WorkingSet> saveWorkingSet(@RequestParam("file") MultipartFile file, @RequestParam("fileName") String fileName) {
        log.info("saveWorkingSet 호출");
        if (file.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        try (InputStreamReader reader = workingSetService.getEncodedReader(file)) {
            if (reader == null) throw new IOException("Failed to create InputStreamReader for the file.");
            
            String fileContentHash = DigestUtils.md5Hex(file.getInputStream());
            UUID existingId = workingSetRepo.findIdByHash(fileContentHash);
            
            if (existingId != null) return ResponseEntity.ok(workingSetRepo.findById(existingId).get());
            else {
                WorkingSet ws = new WorkingSet();
                ws.setName(fileName);
                ws.setHash(fileContentHash);
                ws = workingSetRepo.save(ws);

                RFC4180Parser rfc4180ParserForCsvToBean = new RFC4180ParserBuilder().build();
                CSVReaderBuilder csvReaderBuilderForCsvToBean = new CSVReaderBuilder(reader).withCSVParser(rfc4180ParserForCsvToBean);

                try (CSVReader csvReaderForCsvToBean = csvReaderBuilderForCsvToBean.build()) {
                    List<CSVUploadPattern> parsedData = new CsvToBeanBuilder<CSVUploadPattern>(csvReaderForCsvToBean).withType(CSVUploadPattern.class).build().parse();
                    productPatternService.saveProductPattern(parsedData, ws);
                }

                ws = workingSetRepo.save(ws);
                return ResponseEntity.ok(ws);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
