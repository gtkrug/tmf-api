package edu.gatech.gtri.trustmark.v1_0.impl.io.bulk;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import edu.gatech.gtri.trustmark.v1_0.impl.io.bulk.BulkReadRawData.RawTrustInteroperabilityProfile;
import edu.gatech.gtri.trustmark.v1_0.impl.io.bulk.BulkReadRawData.RawTrustmarkDefinition;
import edu.gatech.gtri.trustmark.v1_0.impl.io.bulk.BulkReadRawData.RawArtifact;
import edu.gatech.gtri.trustmark.v1_0.impl.io.bulk.BulkReadRawData.RawNameDescPair;
import edu.gatech.gtri.trustmark.v1_0.impl.io.bulk.BulkReadRawData.RawTdParameter;

import edu.gatech.gtri.trustmark.v1_0.impl.model.TermImpl;
import edu.gatech.gtri.trustmark.v1_0.io.bulk.ExcelBulkReader;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;

/**
 * Created by Nicholas on 9/6/2016.
 */
public class ExcelBulkReaderImpl extends AbstractBulkReader implements ExcelBulkReader {
    
    // Constants
    private static final Logger logger = Logger.getLogger(ExcelBulkReaderImpl.class);

    public static final RowMapperGetter<TermImpl> GET_TERMS = new RowMapperGetter<TermImpl>() {
        @Override public String getSheetName() { return "Terms"; }
        @Override public AbstractExcelSheetRowMapper<TermImpl> get(File _file, Sheet _sheet) { return new TermsSheetRowMapper(_file, _sheet); }
    };
    public static final RowMapperGetter<RawTrustmarkDefinition> GET_TDS = new RowMapperGetter<RawTrustmarkDefinition>() {
        @Override public String getSheetName() { return "TDs"; }
        @Override public AbstractExcelSheetRowMapper<RawTrustmarkDefinition> get(File _file, Sheet _sheet) { return new TDsSheetRowMapper(_file, _sheet); }
    };
    public static final RowMapperGetter<RawTrustInteroperabilityProfile> GET_TIPS = new RowMapperGetter<RawTrustInteroperabilityProfile>() {
        @Override public String getSheetName() { return "TIPs"; }
        @Override public AbstractExcelSheetRowMapper<RawTrustInteroperabilityProfile> get(File _file, Sheet _sheet) { return new TIPsSheetRowMapper(_file, _sheet); }
    };
    
    // Constructor
    public ExcelBulkReaderImpl() {
        // nothing here right now
    }
    
    // Instance Methods
    @Override
    protected boolean fileIsSupported(File inputFile) {
        String lowerCaseName = inputFile.getName().toLowerCase();
        return lowerCaseName.endsWith(".xls") || lowerCaseName.endsWith(".xlsx");
    }
    
    @Override
    protected void addRawDataFromFile(BulkReadRawData allRawData, File inputFile, int inputFileIndex, Collection<File> allFiles) throws Exception {
        logger.info(String.format("Processing Excel File[%s] for raw data...", inputFile.getName()));
        Workbook excelWorkbook = WorkbookFactory.create(inputFile);
        
        if (excelWorkbook != null) {
            int fileCount = allFiles.size();
            int startPercentage = (100 * inputFileIndex) / fileCount;
            int totalFilePercentage = 100 / fileCount;
            int totalTdPercentage = totalFilePercentage / 2;
            int afterTdBeforeTipPercentage = startPercentage + totalTdPercentage;
            int endPercentage = startPercentage + totalFilePercentage;
            
            List<TermImpl> terms = this.getItemsFromWorkbook(GET_TERMS, inputFile, excelWorkbook, startPercentage, startPercentage);
            allRawData.addTerms(inputFile.getCanonicalPath(), terms);
            
            this.listenerCollection.fireSetMessage(String.format(
                "File %s of %s - Reading Trustmark Definitions",
                inputFileIndex + 1,
                fileCount
            ));
            List<RawTrustmarkDefinition> rawTds = this.getItemsFromWorkbook(GET_TDS, inputFile, excelWorkbook, startPercentage, afterTdBeforeTipPercentage);
            allRawData.addRawTds(rawTds);
    
            this.listenerCollection.fireSetMessage(String.format(
                "File %s of %s - Reading Trust Interoperability Profiles",
                inputFileIndex + 1,
                fileCount
            ));
            List<RawTrustInteroperabilityProfile> rawTips = this.getItemsFromWorkbook(GET_TIPS, inputFile, excelWorkbook, afterTdBeforeTipPercentage, endPercentage);
            allRawData.addRawTips(rawTips);
            
            logger.info(String.format(
                "Finished processing Excel File ['%s']!  Found %s raw TDs, and %s raw TIPs.",
                inputFile.getName(),
                rawTds.size(),
                rawTips.size()
            ));
        }
        else {
            logger.warn(String.format("Could not read Excel workbook into memory from file: %s", inputFile));
        }
    }
    
    public Sheet getSheetFromWorkbook(Workbook excelWorkbook, String sheetName) {
        Sheet result = null;
        if (sheetName != null) {
            int numberOfSheets = excelWorkbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; ++i) {
                Sheet currentSheet = excelWorkbook.getSheetAt(i);
                if (sheetName.equalsIgnoreCase(currentSheet.getSheetName())) {
                    result = currentSheet;
                    break;
                }
            }
        }
        return result;
    }
    
    protected <T> List<T> getItemsFromWorkbook(
        RowMapperGetter<T> rowMapperGetter,
        File inputFile,
        Workbook excelWorkbook,
        int startPercentage,
        int endPercentage
    ) throws Exception {
        ArrayList<T> result = new ArrayList<>();
        String sheetName = rowMapperGetter.getSheetName();
        Sheet sheet = this.getSheetFromWorkbook(excelWorkbook, sheetName);
        if (sheet != null) {
            int percentageRange = endPercentage - startPercentage;
            
            AbstractExcelSheetRowMapper<T> rowMapper = rowMapperGetter.get(inputFile, sheet);
            int lastRowNum = sheet.getLastRowNum();
            result.ensureCapacity(lastRowNum);
            logger.debug(String.format("%s has %s rows", rowMapper.getDebugDescription(), lastRowNum));
    
            this.listenerCollection.fireSetPercentage(startPercentage);
            for (int r = 1; r <= lastRowNum; ++r) {
                Row currentRow = sheet.getRow(r);
                if (currentRow == null) { continue; }
                T currentItem = null;
                try {
                    currentItem = rowMapper.parse(currentRow);
                }catch(Throwable t){
                    logger.error(String.format("Error Processing Row %d from File %s: %s", currentRow.getRowNum()+1, inputFile.getName(), t.toString()));
                    throw t;
                }

                if (rowMapper.isValid(currentItem)) {
                    this.collectTransientDataIfEnabled(rowMapper, currentRow, currentItem);
                    result.add(currentItem);
                } else {
                    logger.debug(String.format("Row %s is invalid", rowMapper.getDebugLocation(currentRow)));
                }
                
                int percentComplete = startPercentage + ((percentageRange * r) / lastRowNum);
                this.listenerCollection.fireSetPercentage(percentComplete);
            }
            this.listenerCollection.fireSetPercentage(endPercentage);
            
        } else {
            logger.debug(String.format("Did not find sheet '%s' in file %s", sheetName, inputFile));
        }
        return result;
    }
    
    protected <T> void collectTransientDataIfEnabled(
        AbstractExcelSheetRowMapper<T> rowMapper,
        Row currentRow,
        T currentItem
    ) throws Exception {
        if (!this.getIsTransientDataCollectionEnabled()) { return; }
    
        RawArtifact rawArtifact;
        if (currentItem instanceof RawTrustmarkDefinition) {
            rawArtifact = ((RawTrustmarkDefinition)currentItem).metadata;
        }
        else if (currentItem instanceof RawArtifact) {
            rawArtifact = (RawArtifact)currentItem;
        }
        else {
            return;
        }
        
        ExcelBulkReadTransientData.collectTransientData(rowMapper, currentRow, rawArtifact);
    }
    
    
    // Static Inner Classes
    public static interface RowMapperGetter<T> {
        public String getSheetName();
        public AbstractExcelSheetRowMapper<T> get(File _file, Sheet _sheet);
    }
    
    public static abstract class AbstractExcelSheetRowMapper<T> {
        
        // Instance Fields
        protected final File file;
        protected final Sheet sheet;
        protected final Map<String,Integer> columnIndicesByLowerCaseName;
        
        // Constructor
        protected AbstractExcelSheetRowMapper(File _file, Sheet _sheet) {
            this.file = _file;
            this.sheet = _sheet;
            this.columnIndicesByLowerCaseName = this.getColumnIndicesByLowerCaseNameFromSheet(this.sheet);
        }
        
        // Instance Methods - Abstract
        public abstract T parse(Row row) throws Exception;
        public abstract boolean isValid(T parsedRow);
        
        // Instance Methods - Concrete
        protected Map<String,Integer> getColumnIndicesByLowerCaseNameFromSheet(Sheet excelSheet) {
            Map<String,Integer> result = new HashMap<>();
            int lastRowNum = excelSheet.getLastRowNum();
            if (lastRowNum > 0) {
                Row firstRow = excelSheet.getRow(0);
                int lastColumnNumber = firstRow.getLastCellNum();
                logger.debug(String.format("%s has %s columns", this.getDebugDescription(), lastColumnNumber));
                for (int c = 0; c < lastColumnNumber; ++c) {
                    Cell currentCell = firstRow.getCell(c);
                    String columnLowerCaseName = "";
                    if (currentCell != null) {
                        columnLowerCaseName = StringUtils.defaultIfBlank(currentCell.getStringCellValue(), "").toLowerCase();
                    }
                    result.put(columnLowerCaseName, c);
                }
            }
            return result;
        }
        
        public String getDebugDescription() {
            return String.format(
                "File[%s]->Sheet[%s]",
                this.file.getName(),
                this.sheet.getSheetName()
            );
        }
        
        protected String getDebugLocation(Row row) {
            return String.format(
                "%s->Row[%s]",
                this.getDebugDescription(),
                row.getRowNum()
            );
        }
    
        public int getIndexFor(String columnName) {
            String lowerCaseColumnName = BulkImportUtils.defaultTrim(columnName).toLowerCase();
            Integer result = AbstractExcelSheetRowMapper.this.columnIndicesByLowerCaseName.get(columnName.toLowerCase());
            if (result == null || result < 0) {
                result = -1;
                logger.warn(String.format(
                    "%s does not have column '%s'.",
                    this.getDebugDescription(),
                    columnName
                ));
            }
            return result;
        }
        
        public String getValueFor(Row row, String columnName) {
            int index = this.getIndexFor(columnName);
            return this.getValueFor(row, index);
        }
        
        public String getValueFor(Row row, int index) {
            String result = null;
            if (index < 0) {
                return null;
            }
            Cell cell = row.getCell(index);
            if (cell != null) {
                result = cell.toString();
            }
            return result;
        }
        
        public Map<String,String> getAllValuesByLowerCaseName(Row row) {
            Map<String, String> result = new HashMap<>();
            Set<String> lowerCaseColumnNames = this.columnIndicesByLowerCaseName.keySet();
            for (String lowerCaseColumnName : lowerCaseColumnNames) {
                String columnValue = this.getValueFor(row, lowerCaseColumnName);
                result.put(lowerCaseColumnName, columnValue);
            }
            return result;
        }
        
        // Instance Inner Classes
        protected abstract class MappedColumn<V> {
            // Instance Fields
            public final String name;
            public final int index;
            
            // Constructor
            public MappedColumn(String _name) {
                this.name = _name;
                this.index = AbstractExcelSheetRowMapper.this.getIndexFor(this.name);
            }
            
            // Instance Methods
            public abstract V getFor(Row row);
            
            public String getStringFor(Row row) {
                return AbstractExcelSheetRowMapper.this.getValueFor(row, this.index);
            }
        }
        
        protected class StringColumn extends MappedColumn<String> {
            public StringColumn(String _name) {
                super(_name);
            }
    
            @Override
            public String getFor(Row row) {
                return this.getStringFor(row);
            }
        }
        
        protected class FilteredStringColumn extends MappedColumn<String> {
            public FilteredStringColumn(String _name) {
                super(_name);
            }
            
            @Override
            public String getFor(Row row) {
                return BulkImportUtils.filterHumanReadableText(this.getStringFor(row));
            }
        }
        
        protected class PipeListColumn extends MappedColumn<List<String>> {
            public PipeListColumn(String _name) {
                super(_name);
            }
    
            @Override
            public List<String> getFor(Row row) {
                return BulkImportUtils.parsePipeFormat(this.getStringFor(row));
            }
        }
        
        protected class ColonPipeListColumn extends MappedColumn<List<RawNameDescPair>> {
            public ColonPipeListColumn(String _name) {
                super(_name);
            }
    
            @Override
            public List<RawNameDescPair> getFor(Row row) {
                return BulkImportUtils.parseColonPipeFormat(this.getStringFor(row));
            }
        }
        
        protected class ColonPipeMapColumn extends MappedColumn<Map<String,String>> {
            public ColonPipeMapColumn(String _name) {
                super(_name);
            }
    
            @Override
            public Map<String,String> getFor(Row row) {
                return BulkImportUtils.explodeListNameValuePairs(this.getStringFor(row));
            }
        }
        
        protected class FilteredValueColonPipeMapColumn extends MappedColumn<Map<String,String>> {
            public FilteredValueColonPipeMapColumn(String _name) {
                super(_name); this.__nameHelp = _name;
            }

            private String __nameHelp;
            
            @Override
            public Map<String,String> getFor(Row row) {
                Map<String, String> unfilteredResult = BulkImportUtils.explodeListNameValuePairs(this.getStringFor(row));
                Map<String, String> result = new HashMap<>();
                for (String key : unfilteredResult.keySet()) {
                    String value = unfilteredResult.get(key);
                    String filteredValue = BulkImportUtils.filterHumanReadableText(value);
                    result.put(key, filteredValue);
                }
                return result;
            }
        }
        
        protected class ParameterListColumn extends MappedColumn<List<RawTdParameter>> {
            public ParameterListColumn(String _name) {
                super(_name);
            }
    
            @Override
            public List<RawTdParameter> getFor(Row row) {
                return BulkImportUtils.parseParameterPipeFormat(this.getStringFor(row));
            }
        }
        
    }
    
    public static class TermsSheetRowMapper extends AbstractExcelSheetRowMapper<TermImpl> {
        // Instance Fields
        public final FilteredStringColumn TERM_NAME = new FilteredStringColumn("Term Name");
        public final FilteredStringColumn TERM_DEFINITION = new FilteredStringColumn("Term Definition");
        public final PipeListColumn ABBREVIATIONS = new PipeListColumn("Abbreviations");
    
        // Constructor
        public TermsSheetRowMapper(File _file, Sheet _sheet) {
            super(_file, _sheet);
        }
    
        // Instance Methods
        @Override
        public TermImpl parse(Row row) throws Exception {
            TermImpl result = new TermImpl();
            result.setName(this.TERM_NAME.getFor(row));
            result.setDefinition(this.TERM_DEFINITION.getFor(row));
            if (StringUtils.isBlank(result.getDefinition())) {
                throw new UnsupportedOperationException(String.format(
                    "Term[%s] in %s has no definition. Each defined term MUST have a definition.",
                    result.getName(),
                    this.getDebugLocation(row)
                ));
            }
            result.setAbbreviations(new HashSet<>(this.ABBREVIATIONS.getFor(row)));
            return result;
        }
        
        @Override
        public boolean isValid(TermImpl term) {
            return term != null && term.getName() != null;
        }
    }
    
    public static class TDsSheetRowMapper extends AbstractExcelSheetRowMapper<RawTrustmarkDefinition> {
        // Constants
        public static final String DEFAULT_ISSUANCE_CRITERIA = "yes(ALL)";
        
        // Instance Fields
        public final FilteredStringColumn STEP_NAME = new FilteredStringColumn("Step Name");
        public final FilteredStringColumn STEP_DESC = new FilteredStringColumn("Step Desc");
        public final FilteredStringColumn CRITERION_NAME = new FilteredStringColumn("Criterion Name");
        public final FilteredStringColumn CRITERION_DESC = new FilteredStringColumn("Criterion Desc");
        public final StringColumn TD_MONIKER = new StringColumn("TD Moniker");
        public final FilteredStringColumn TD_NAME = new FilteredStringColumn("TD Name");
        public final StringColumn TD_VERSION = new StringColumn("TD Version");
        public final FilteredStringColumn TD_DESC = new FilteredStringColumn("TD Description");
        public final StringColumn TD_PUB_DATE = new StringColumn("TD Publication DateTime");
        public final StringColumn STAKEHOLDER_DESC = new StringColumn("Stakeholder Desc");
        public final StringColumn RECIPIENT_DESC = new StringColumn("Recipient Desc");
        public final StringColumn RELYING_PARTY_DESC = new StringColumn("Relying Party Desc");
        public final StringColumn PROVIDER_DESC = new StringColumn("Provider Desc");
        public final StringColumn PROVIDER_ELIGIBILITY_CRITERIA = new StringColumn("Provider Eligibility Criteria");
        public final StringColumn ASSESSOR_QUALIFICATIONS_DESC = new StringColumn("Assessor Qualifications Desc");
        public final StringColumn REVOCATION_CRITERIA = new StringColumn("Trustmark Revocation Criteria");
        public final StringColumn EXTENSION_DESC = new StringColumn("Extension Description");
        public final StringColumn NOTES = new StringColumn("Notes");
        public final StringColumn LEGAL_NOTICE = new StringColumn("Legal Notice");
        public final StringColumn CRITERIA_PREFACE = new StringColumn("Criteria Preface");
        public final StringColumn ASSESSMENT_PREFACE = new StringColumn("Assessment Preface");
        public final StringColumn ISSUANCE_CRITERIA = new StringColumn("Issuance Criteria");
        public final PipeListColumn KEYWORDS = new PipeListColumn("Keywords");
        public final PipeListColumn SUPERSEDES = new PipeListColumn("Superseded TD");
        public final PipeListColumn TERMS_INCLUDE = new PipeListColumn("Terms Include");
        public final PipeListColumn TERMS_EXCLUDE = new PipeListColumn("Terms Exclude");
        public final PipeListColumn TIPS = new PipeListColumn("TIPs");
        public final ColonPipeListColumn CITATIONS = new ColonPipeListColumn("Citations");
        public final ColonPipeListColumn ARTIFACTS = new ColonPipeListColumn("Artifacts");
        public final ColonPipeListColumn TERMS = new ColonPipeListColumn("Terms");
        public final FilteredValueColonPipeMapColumn SOURCES = new FilteredValueColonPipeMapColumn("Sources");
        public final ParameterListColumn PARAMETERS = new ParameterListColumn("Parameters");
        
        // Constructor
        public TDsSheetRowMapper(File _file, Sheet _sheet) {
            super(_file, _sheet);
        }
        
        @Override
        public RawTrustmarkDefinition parse(Row row) throws Exception {
            logger.debug("Parsing " + this.getDebugLocation(row));
            String name = this.TD_NAME.getFor(row);
            logger.debug("  TD Name: "+name);
            String moniker = this.TD_MONIKER.getFor(row);
            if (StringUtils.isNotBlank(name) && StringUtils.isBlank(moniker)) {
                moniker = BulkImportUtils.generateMoniker(name);
            }
            if (moniker != null) {
                moniker = BulkImportUtils.cleanseMoniker(moniker);
            }
    
            String stepName = this.STEP_NAME.getFor(row);
            String critName = this.CRITERION_NAME.getFor(row);
            if (StringUtils.isBlank(stepName) && StringUtils.isBlank(critName)) {
                logger.warn(String.format(
                    "%s has no meaningful value.  Columns %s and %s are both empty, and at least 1 is required to exist.",
                    this.getDebugLocation(row),
                    this.STEP_NAME.name,
                    this.CRITERION_NAME.name
                ));
                return null;
            }
    
            RawTrustmarkDefinition rawTd = new RawTrustmarkDefinition();
            rawTd.rowIndex = row.getRowNum();
            rawTd.metadata.rowIndex = row.getRowNum();
            rawTd.metadata.excelFile = this.file.getCanonicalPath();
            rawTd.metadata.debugLocation = this.getDebugLocation(row);
    
            if (StringUtils.isNotBlank(stepName)) {
                logger.debug("  Found assessment step name: " + stepName);
                rawTd.assessmentStep.id = BulkImportUtils.newUniqueId(stepName);
                rawTd.assessmentStep.name = stepName;
                rawTd.assessmentStep.description = this.STEP_DESC.getFor(row);
                logger.debug("     And step description: "+rawTd.assessmentStep.description);
                rawTd.assessmentStep.artifacts.addAll(this.ARTIFACTS.getFor(row));
                rawTd.assessmentStep.parameters.addAll(this.PARAMETERS.getFor(row));
            }
    
            if (StringUtils.isNotBlank(critName)) {
                logger.debug("  Found criterion name: " + critName);
                rawTd.criterion.id = BulkImportUtils.newUniqueId(critName) + "Criterion";
                rawTd.criterion.name = critName;
                rawTd.criterion.description = this.CRITERION_DESC.getFor(row);
                rawTd.criterion.citations.addAll(this.CITATIONS.getFor(row));
                logger.debug("    Citations: " + rawTd.criterion.citations);
                
                // In case a citation references new sources on a secondary (unnamed) TD row
                rawTd.metadata.sources.putAll(this.SOURCES.getFor(row));
            }
    
            if (StringUtils.isNotBlank(name)) {
                // Strings
                rawTd.metadata.id = BulkImportUtils.newUniqueId(null);
                rawTd.metadata.originalMoniker = this.TD_MONIKER.getFor(row);
                rawTd.metadata.moniker = moniker;
                rawTd.metadata.name = name;
                rawTd.metadata.version = this.TD_VERSION.getFor(row);
                rawTd.metadata.description = this.TD_DESC.getFor(row);
                rawTd.metadata.publicationDateTime = this.TD_PUB_DATE.getFor(row);
                rawTd.metadata.stakeholderDesc = this.STAKEHOLDER_DESC.getFor(row);
                rawTd.metadata.recipientDesc = this.RECIPIENT_DESC.getFor(row);
                rawTd.metadata.relyingPartyDesc = this.RELYING_PARTY_DESC.getFor(row);
                rawTd.metadata.providerDesc = this.PROVIDER_DESC.getFor(row);
                rawTd.metadata.providerEligibilityCriteria = this.PROVIDER_ELIGIBILITY_CRITERIA.getFor(row);
                rawTd.metadata.assessorQualificationsDesc = this.ASSESSOR_QUALIFICATIONS_DESC.getFor(row);
                rawTd.metadata.revocationCriteria = this.REVOCATION_CRITERIA.getFor(row);
                rawTd.metadata.extensionDesc = this.EXTENSION_DESC.getFor(row);
                rawTd.metadata.notes = this.NOTES.getFor(row);
                rawTd.metadata.legalNotice = this.LEGAL_NOTICE.getFor(row);
                rawTd.metadata.criteriaPreface = this.CRITERIA_PREFACE.getFor(row);
                rawTd.metadata.assessmentPreface = this.ASSESSMENT_PREFACE.getFor(row);
                rawTd.metadata.issuanceCriteria = this.ISSUANCE_CRITERIA.getFor(row);
                // Pipe Lists
                rawTd.metadata.keywords.addAll(this.KEYWORDS.getFor(row));
                rawTd.metadata.supersedesUris.addAll(this.SUPERSEDES.getFor(row));
                rawTd.metadata.termsInclude.addAll(BulkImportUtils.lowerCaseAll(this.TERMS_INCLUDE.getFor(row)));
                rawTd.metadata.termsExclude.addAll(BulkImportUtils.lowerCaseAll(this.TERMS_EXCLUDE.getFor(row)));
                rawTd.metadata.tips.addAll(this.TIPS.getFor(row));
                // Colon+Pipe Lists
                rawTd.metadata.terms.addAll(this.TERMS.getFor(row));
            }
            
            //logger.debug("  Parsed rawTd: " + rawTd.toJson().toString(2));
            return rawTd;
        }
    
        @Override
        public boolean isValid(RawTrustmarkDefinition rawTd) {
            return rawTd != null;
        }
    }
    
    public static class TIPsSheetRowMapper extends AbstractExcelSheetRowMapper<RawTrustInteroperabilityProfile> {
        // Instance Fields
        public final StringColumn TIP_CATEGORY = new StringColumn("Category");
        public final FilteredStringColumn TIP_NAME = new FilteredStringColumn("TIP Name");
        public final FilteredStringColumn TIP_DESC = new FilteredStringColumn("TIP Description");
        public final StringColumn TIP_VERSION = new StringColumn("TIP Version");
        public final StringColumn TIP_TRUST_EXPRESSION = new StringColumn("Trust Expression");
        public final StringColumn TIP_ID = new StringColumn("TIP Id");
        public final StringColumn TIP_ISSUER_NAME = new StringColumn("Issuer Name");
        public final StringColumn TIP_ISSUER_ID = new StringColumn("Issuer ID");
        public final StringColumn TIP_PRIMARY = new StringColumn("Primary");
        public final StringColumn TIP_MONIKER = new StringColumn("TIP Moniker");
        public final StringColumn NOTES = new StringColumn("Notes");
        public final StringColumn LEGAL_NOTICE = new StringColumn("Legal Notice");
        public final StringColumn TIP_PUB_DATE = new StringColumn("TIP Publication DateTime");
        public final PipeListColumn SUPERSEDES = new PipeListColumn("Superseded TIP");
        public final ColonPipeListColumn TERMS = new ColonPipeListColumn("Terms");
        public final PipeListColumn TERMS_INCLUDE = new PipeListColumn("Terms Include");
        public final PipeListColumn TERMS_EXCLUDE = new PipeListColumn("Terms Exclude");
        public final PipeListColumn KEYWORDS = new PipeListColumn("Keywords");
        public final FilteredValueColonPipeMapColumn SOURCES = new FilteredValueColonPipeMapColumn("Sources");
        
        // Constructor
        public TIPsSheetRowMapper(File _file, Sheet _sheet) {
            super(_file, _sheet);
        }
    
        @Override
        public RawTrustInteroperabilityProfile parse(Row row) throws Exception {
            RawTrustInteroperabilityProfile rawTip = null;
            String name = this.TIP_NAME.getFor(row);
            if (StringUtils.isNotBlank(name) && !name.trim().equals("$")) {
                rawTip = new RawTrustInteroperabilityProfile();
                rawTip.rowIndex = row.getRowNum();
                rawTip.excelFile = this.file.getCanonicalPath();
                rawTip.debugLocation = this.getDebugLocation(row);

                rawTip.category = this.TIP_CATEGORY.getFor(row);
                rawTip.name = name;
                rawTip.description = this.TIP_DESC.getFor(row);
                rawTip.trustExpression = this.TIP_TRUST_EXPRESSION.getFor(row);
                rawTip.version = this.TIP_VERSION.getFor(row);
                rawTip.notes = this.NOTES.getFor(row);
                rawTip.legalNotice = this.LEGAL_NOTICE.getFor(row);
                rawTip.publicationDateTime = this.TIP_PUB_DATE.getFor(row);
                rawTip.primary = this.TIP_PRIMARY.getFor(row);
                String moniker = this.TIP_MONIKER.getFor(row);

                if (StringUtils.isNotBlank(name) && StringUtils.isBlank(moniker)) {
                    moniker = BulkImportUtils.generateMoniker(name);
                }
                if (moniker != null) {
                    rawTip.moniker = BulkImportUtils.cleanseMoniker(moniker);
                }

                rawTip.terms.addAll(this.TERMS.getFor(row));
                rawTip.termsInclude.addAll(BulkImportUtils.lowerCaseAll(this.TERMS_INCLUDE.getFor(row)));
                rawTip.termsExclude.addAll(BulkImportUtils.lowerCaseAll(this.TERMS_EXCLUDE.getFor(row)));
                rawTip.sources.putAll(this.SOURCES.getFor(row));
                rawTip.keywords.addAll(this.KEYWORDS.getFor(row));

                logger.info(String.format("    TIP Row #%s has TIP: %s", row.getRowNum() + 1, name));
                // TODO: There are other columns in the spreadsheet, figure those out later.
            } else {
                logger.info(String.format(
                    "    TIP Row #%s [sheet %s] has no valid name; must not be a TIP metadata row.",
                    row.getRowNum() + 1,
                    this.sheet.getSheetName()
                ));
            }
            //logger.debug("  Parsed rawTip: " + rawTip.toJson().toString(2));
            return rawTip;
        }
    
        @Override
        public boolean isValid(RawTrustInteroperabilityProfile rawTip) {
            return rawTip != null;
        }
    }
    
    public static class ExcelBulkReadTransientData {
        // Constants
        public static final String TRANSIENT_DATA_ALL_VALUES_BY_LOWER_CASE_NAME = "ALL_VALUES_BY_LOWER_CASE_NAME";
        public static final String TRANSIENT_DATA_FILE_NAME = "FILE_NAME";
        public static final String TRANSIENT_DATA_SHEET_NAME = "SHEET_NAME";
        public static final String TRANSIENT_DATA_ROW_NUMBER = "ROW_NUMBER";
        
        // Fields
        public final Map<String, String> allValuesByLowerCaseName;
        public final String fileName;
        public final String sheetName;
        public final int rowNumber;
        
        // Constructor
        public ExcelBulkReadTransientData(BulkReadArtifact artifact) throws Exception {
            Map<String, Object> transientDataMap = artifact.getTransientDataMap();
            
            @SuppressWarnings("unchecked")
            Map<String, String> _allValuesByLowerCaseName = (Map<String, String>)transientDataMap.get(TRANSIENT_DATA_ALL_VALUES_BY_LOWER_CASE_NAME);
            this.allValuesByLowerCaseName = Collections.unmodifiableMap(_allValuesByLowerCaseName);
            
            this.fileName = (String)transientDataMap.get(TRANSIENT_DATA_FILE_NAME);
            this.sheetName = (String)transientDataMap.get(TRANSIENT_DATA_SHEET_NAME);
            
            Integer _rowNumber = (Integer)transientDataMap.get(TRANSIENT_DATA_ROW_NUMBER);
            this.rowNumber = (_rowNumber == null) ? -1 : _rowNumber;
        }
        
        // Static Methods
        public static void collectTransientData(AbstractExcelSheetRowMapper<?> rowMapper, Row currentRow, RawArtifact rawArtifact) throws Exception {
            Map<String, String> allValuesByLowerCaseName = rowMapper.getAllValuesByLowerCaseName(currentRow);
            rawArtifact.transientDataMap.put(TRANSIENT_DATA_ALL_VALUES_BY_LOWER_CASE_NAME, allValuesByLowerCaseName);
            rawArtifact.transientDataMap.put(TRANSIENT_DATA_FILE_NAME, rowMapper.file.getCanonicalPath());
            rawArtifact.transientDataMap.put(TRANSIENT_DATA_SHEET_NAME, rowMapper.sheet.getSheetName());
            rawArtifact.transientDataMap.put(TRANSIENT_DATA_ROW_NUMBER, currentRow.getRowNum() + 1);
        }
    }
}
