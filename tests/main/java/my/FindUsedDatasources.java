package my;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parse spring structure
 *
 * http://jira.pulse.corp/browse/ET-70
 *
 *
 * todo [!] now only one value is taken into account from properties, it can be several
 *
 * @author Pavel Moukhataev
 */
public class FindUsedDatasources {

    private static final Logger LOG = LogManager.getLogger();

    private static final int MAX_PROJECT_SEARCH_DEEP = 4;



    private Map<String, Project> projects = new HashMap<>();
    private Map<String, List<DataserviceFile>> dataservicesConfigurations = new HashMap<>();
    private Map<String, Set<String>> globalProperties = new HashMap<>();

    /**
     * List of [config] folders that contain dataset files.
     *
     * dataset.properties files are used to discover
     */
    private Set<String> datasetSrcFolders = new HashSet<>();


    private Set<String> httpdLogDatasets = new HashSet<>();
    private Map<Integer, Set<String>> httpdUnknownStatusCode = new HashMap<>();
    private Map<String, String> datasetSrcFiles = new HashMap<>();
    private Map<String, AtomicInteger> datasetSrcBadCsvCount = new HashMap<>();
    private Map<String, Set<String>> datasetSrcBadExtension = new HashMap<>();
    private Map<String, String> datasetDstFiles = new HashMap<>();
    private Map<String, AtomicInteger> datasetDstBadCsvCount = new HashMap<>();



    private void find() throws IOException {
        // Cycle through projects to find all spring files for this project
        findProject(new File("/opt/projects/pulsepoint/ad-serving-and-commons"));
        readGlobalProperties(new File("/opt/projects/pulsepoint/ad-serving-configuration"));
        findHttpdLogs(new File("/opt/projects/pulsepoint/pp-internals/test-data/src/misc/ET-70-dataSetLogs/logs"));
        readDatasetDstFile(new File("/opt/projects/pulsepoint/pp-internals/test-data/src/misc/ET-70-dataSetLogs/dirs/ds-dst.txt"));
        readDatasetDstFile(new File("/opt/projects/pulsepoint/pp-internals/test-data/src/misc/ET-70-dataSetLogs/dirs/dsd-dst.txt"));
        readDatasetSrcFile(new File("/opt/projects/pulsepoint/pp-internals/test-data/src/misc/ET-70-dataSetLogs/dirs/ds-src1.txt"));
        readDatasetSrcFile(new File("/opt/projects/pulsepoint/pp-internals/test-data/src/misc/ET-70-dataSetLogs/dirs/ds-src2.txt"));
        readDatasetSrcFile(new File("/opt/projects/pulsepoint/pp-internals/test-data/src/misc/ET-70-dataSetLogs/dirs/dsd-src.txt"));

        // Find all spring files (including imports) for each project
        for (Project project : projects.values()) {
            Set<Project> usedProjects = new HashSet<>();
            addProjectsDependencies(usedProjects, project.dependencies);
            usedProjects.add(project);
            project.allDependencies = usedProjects;

            Map<String, SpringFile> springFiles = new HashMap<>();
            for (SpringFile springFile : project.springFiles.values()) {
                addSpringFile(springFile, springFiles, usedProjects);
            }
            project.allSpringFiles = springFiles.values();
        }


        // Let's check repositoryManagerBeans
        List<RepositoryBean> repositoryBeanList = new ArrayList<>();
        Map<String, Set<RepositoryBean>> datasetRepositoryBeanMap = new HashMap<>();

        for (Project project : projects.values()) {
//            LOG.trace("Project {}", project.name);

            // Process repository beans
            for (SpringFile springFile : project.springFiles.values()) {
                for (SpringBean springBean : springFile.springBeansList) {
                    if (springBean.isAbstract) continue;

                    RepositoryBean repositoryBean = null;
                    try {
                        String beanClass = springBean.getBeanClass(project);
//                        LOG.trace("Bean {} class {}" ,springBean.beanId, beanClass);
                        if ("com.contextweb.commons.data.repository.manager.RepositoryDefinition".equals(beanClass)) {
                            repositoryBean = readRepositoryDefinition(project, springBean);
                        } else if ("com.contextweb.commons.data.repository.MultiValueRepositoryDefinitionBuilder".equals(beanClass)) {
                            repositoryBean = readMultivalueRepositoryDefinition(project, springBean);
                        } else if ("com.contextweb.commons.data.repository.manager.RepositoryManager".equals(beanClass)) {
                            readRepositoryManager(springBean);
                        }

                        if (repositoryBean != null) {
                            repositoryBeanList.add(repositoryBean);
                            for (String dataset : repositoryBean.datasets) {
                                Set<RepositoryBean> repositoryBeanSet = datasetRepositoryBeanMap.get(dataset);
                                if (repositoryBeanSet == null) {
                                    repositoryBeanSet = new HashSet<>();
                                    datasetRepositoryBeanMap.put(dataset, repositoryBeanSet);
                                }
                                repositoryBeanSet.add(repositoryBean);
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Error processing bean {}", springBean, e);
                    }
                }
            }
        }

        Set<RepositoryBean> explicitlyListedRepositoryBeanSet = new HashSet<>();
        for (Project project : projects.values()) {
            for (String repositoryDefinitionBeanId : project.repositoryManagerBeans) {
                RepositoryBean repositoryBean = project.findRepositoryBean(repositoryDefinitionBeanId);
                if (repositoryBean == null) {
                    LOG.warn("Repository bean not found {}", repositoryDefinitionBeanId);
                    continue;
                }
                repositoryBean.projectsUsed.add(project);
                explicitlyListedRepositoryBeanSet.add(repositoryBean);
            }
        }

        //
//        LOG.debug("Repository Bean List: {}", repositoryBeanList);

        // Find real datasets -> existedDataservices
        findAllDataservice_ConfigurationsFiles();

        LOG.info("Implicitly listed beans: {}", repositoryBeanList.stream().filter(b -> !explicitlyListedRepositoryBeanSet.contains(b)).collect(Collectors.toSet()));
        LOG.info("Unused beans: {}", repositoryBeanList.stream().filter(r -> r.projectsUsed.isEmpty()).collect(Collectors.toSet()));

        LOG.info("Unused datasets: {}", dataservicesConfigurations.entrySet().stream().filter(d -> !datasetRepositoryBeanMap.containsKey(d.getKey())).flatMap(d -> d.getValue().stream()).map(f -> f.file).collect(Collectors.toSet()));
        LOG.info("Not found beans datasets: {}", datasetRepositoryBeanMap.entrySet().stream().filter(k -> !dataservicesConfigurations.containsKey(k.getKey())).flatMap(d -> d.getValue().stream()).collect(Collectors.toSet()));

/*
        LOG.info("Used and found datasets");
        for (Map.Entry<String, Set<RepositoryBean>> stringSetEntry : datasetRepositoryBeanMap.entrySet()) {
            String datasetName = stringSetEntry.getKey();
            DataserviceFile dataserviceFile = dataservicesConfigurations.get(datasetName);
            if (dataserviceFile != null) {
                Set<RepositoryBean> beanSet = stringSetEntry.getValue();
                LOG.info("    {}|{}|{}", dataserviceFile.file, dataserviceFile.name, beanSet);
            }
        }
*/


/*
        LOG.info("Used but not found datasets");
        for (Map.Entry<String, Set<RepositoryBean>> stringSetEntry : datasetRepositoryBeanMap.entrySet()) {
            String datasetName = stringSetEntry.getKey();
            DataserviceFile dataserviceFile = dataservicesConfigurations.get(datasetName);
            if (dataserviceFile == null) {
                Set<RepositoryBean> beanSet = stringSetEntry.getValue();
                LOG.info("    {}|{}", datasetName, beanSet);
            }
        }
*/

        removeDatacenter(httpdLogDatasets);
        removeDatacenter(datasetRepositoryBeanMap);
        removeDatacenter(datasetSrcFiles);
        removeDatacenter(datasetDstFiles);
        removeDatacenter(dataservicesConfigurations);


        Set<String> usedDatasources = new HashSet<>();
        usedDatasources.addAll(httpdLogDatasets);
        usedDatasources.addAll(datasetRepositoryBeanMap.keySet());


        checkEverythingFound("src", "dest", datasetSrcFiles, datasetDstFiles.keySet());
        checkEverythingFound("httpLog", "dest", httpdLogDatasets, datasetDstFiles.keySet());
        checkEverythingFound("dataservices-configuration", "dest", dataservicesConfigurations.keySet(), datasetDstFiles.keySet());
        checkEverythingFound("spring", "httpLog", datasetRepositoryBeanMap.keySet(), httpdLogDatasets);
        checkEverythingFound("spring", "dest", datasetRepositoryBeanMap.keySet(), datasetDstFiles.keySet());


        displayMap("Not found http files", httpdUnknownStatusCode);

        Map<String, String> unusedDstFiles = new HashMap<>(datasetDstFiles);
        unusedDstFiles.keySet().removeAll(usedDatasources);
//        LOG.info("DST files not used: {}", unusedDstFiles);
        displayMapReverted("Unused DST files", unusedDstFiles);
        displayMap("DST files with strange extension like __64RWNdEpeiC3__gz", datasetDstBadCsvCount);

        Map<String, String> unusedSrcFiles = new HashMap<>(datasetSrcFiles);
        unusedSrcFiles.keySet().removeAll(usedDatasources);
//        LOG.info("SRC datasets not used: {}", unusedSrcFiles);
        displayMapReverted("Unused SRC files", unusedSrcFiles);
        displayMap("SRC files with strange extension like .csv__03pOKxCMmvlk", datasetSrcBadCsvCount);
        displayMap("SRC files with unknown extension", datasetSrcBadExtension);


        Map<String, List<DataserviceFile>> unusedDataservicesConfigurations = new HashMap<>(dataservicesConfigurations);
        unusedDataservicesConfigurations.keySet().removeAll(usedDatasources);
//        LOG.info("Files datasets not used: {}", unusedDataservicesConfigurations.keySet());

        ;
        displayMap("Unused dataservice-configuration files",
                unusedDataservicesConfigurations.values().stream().flatMap(Collection::stream)
                        .collect(Collectors.toMap(kv -> kv.file.getParentFile().getAbsolutePath(), kv -> Collections.singleton(kv.file.getName()),
                                (s1,s2) -> {
                                    Set<String> r = new TreeSet<>(s1);
                                    r.addAll(s2);
                                    return r;
                                }
                                , TreeMap::new))
        );

    }

    private static final Pattern dataCenterPattern = Pattern.compile("([_\\.])(?:lga|sjc|ams)([_\\.]|$)", Pattern.CASE_INSENSITIVE);

    private void removeDatacenter(Set<String> set) {
        Set<String> added = new HashSet<>();
        for (Iterator<String> it = set.iterator(); it.hasNext();) {
            String next = it.next();
            String result = dataCenterPattern.matcher(next).replaceAll("$1\\${DC}$2");
            if (result != next) {
                it.remove();
                added.add(result);
            }
        }
        set.addAll(added);
    }

    private <T> void removeDatacenter(Map<String, T> map) {
        Map<String, T> added = new HashMap<>();
        for (Iterator<Map.Entry<String, T>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, T> entry = it.next();
            String key = entry.getKey();
            String result = dataCenterPattern.matcher(key).replaceAll("$1\\${DC}$2");
            if (result != key) {
                it.remove();
                added.put(result, entry.getValue());
            }
        }
        map.putAll(added);
    }

    private void checkEverythingFound(String nameSrc, String nameDst, Map<String, String> found, Set<String> original) {
        Map<String, String> checkEverythingFound = new HashMap<>(found);
        checkEverythingFound.keySet().removeAll(original);
        if (!checkEverythingFound.isEmpty()) {
            LOG.error("Invalid! Something from {} was not found in {} : {}", nameSrc, nameDst, checkEverythingFound.keySet());
            displayMapReverted("Something from " + nameSrc + " not found in " + nameDst, checkEverythingFound);
        }
    }

    private void checkEverythingFound(String nameSrc, String nameDst, Set<String> found, Set<String> original) {
        Set<String> checkEverythingFound = new HashSet<>(found);
        checkEverythingFound.removeAll(original);
        if (!checkEverythingFound.isEmpty()) {
            LOG.error("Invalid! Something from {} was not found in {} : {}", nameSrc, nameDst, checkEverythingFound);
        }
    }

    private void displayMapReverted(String name, Map<String, String> unordered) {
        Map<String, Set<String>> ordered = new TreeMap<>();
        for (Map.Entry<String, String> stringStringEntry : unordered.entrySet()) {
            String dataset = stringStringEntry.getKey();
            String key = stringStringEntry.getValue();
            Set<String> strings = ordered.get(key);
            if (strings == null) {
                strings = new TreeSet<>();
                ordered.put(key, strings);
            }
            strings.add(dataset);
        }
        displayMap(name, ordered);
    }

    private void displayMap(String name, Map<?, ?> ordered) {
        LOG.info(name);
        for (Map.Entry<?, ?> stringSetEntry : ordered.entrySet()) {
            LOG.info("    {}", stringSetEntry.getKey());
            LOG.info("        {}", stringSetEntry.getValue());
        }
    }

    private void readDatasetDstFile(File file) throws IOException {
        Set<String> unknownDstFiles = new HashSet<>();

        BufferedReader in = new BufferedReader(new FileReader(file));
        String inLine;
        while ((inLine = in.readLine()) != null) {
            if (inLine.isEmpty() || inLine.charAt(0) != '-') continue;
            String[] parts = inLine.split(" +");
            String datasetName = parts[8];
            int __pos = datasetName.indexOf("__");
            if (__pos != -1 && datasetName.substring(__pos + 14, __pos + 16).equals("__")) {
//                datasetName = datasetName.substring(0, datasetName.length() - 16);
                get(datasetDstBadCsvCount, file.getName(), k -> new AtomicInteger()).incrementAndGet();
                continue;
            }
            int dotIndex = datasetName.lastIndexOf('.');
            if (dotIndex != -1) {
                String extension = datasetName.substring(dotIndex+1);
                switch (extension) {
                    case "tar":
                        datasetDstFiles.put(datasetName, file.getName());
                        break;
                    case "gz":
                    case "md5":
                    case "metadata":
                    case "xml":
                    case "json":
                        datasetName = datasetName.substring(0, dotIndex);
                        datasetDstFiles.put(datasetName, file.getName());
                        break;
                    default:
//                    LOG.debug("Unknown extension dst: {}", extension);
                        break;
                }
            }
            unknownDstFiles.add(datasetName);
        }
        in.close();

        unknownDstFiles.removeAll(datasetDstFiles.keySet());
        if (!unknownDstFiles.isEmpty()) {
            LOG.error("Unknwon DST [{}] files: {}", file.getName(), unknownDstFiles);
        }
    }

    /**
     * see com.contextweb.data.service.resource.builder.DataResourceBuilderStandard#buildDataResource(java.io.File)
     *
     * allowed extensions: sql, txt, csv, tar, json, xml, metadata
     */
    private void readDatasetSrcFile(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String inLine;
        boolean dirPresentInConfig = false;
        String dir = null;
        while ((inLine = in.readLine()) != null) {
            if (inLine.isEmpty()) continue;
            if (inLine.charAt(0) == '/') {
                dir = inLine.substring(0, inLine.length()-1);
                dirPresentInConfig = datasetSrcFolders.contains(dir);
                continue;
            }

            if (!dirPresentInConfig || inLine.charAt(0) != '-') continue;

            String[] parts = inLine.split(" +");
            String datasetName = parts[8];
            int dotIndex = datasetName.lastIndexOf('.');
            if (dotIndex != -1) {
                String extension = datasetName.substring(dotIndex+1);
                if (extension.equals("tar")) {
                    datasetSrcFiles.put(datasetName, dir);
                } else if (extension.equals("txt") || extension.equals("csv") || extension.equals("sql") || extension.equals("xml") || extension.equals("json")) {
                    datasetName = datasetName.substring(0, dotIndex);
                    datasetSrcFiles.put(datasetName, dir);
                } else if (extension.startsWith("csv__")) {
//                    LOG.debug("Unknown extension src: {}", extension);
                    get(datasetSrcBadCsvCount, dir, k -> new AtomicInteger()).incrementAndGet();
                } else {
                    get(datasetSrcBadExtension, dir, k -> new TreeSet<>()).add(datasetName);
                }
            } else {
//                LOG.debug("No extension src: {}", datasetName);
                get(datasetSrcBadExtension, dir, k -> new TreeSet<>()).add(datasetName);
            }
        }
        in.close();
    }

    private <K, V> V get(Map<K, V> map, K key, Function<K, V> create) {
        V v = map.get(key);
        if (v == null) {
            v = create.apply(key);
            map.put(key, v);
        }
        return v;
    }

    private void findHttpdLogs(File dir) throws IOException {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                findHttpdLogs(file);
            } else if (file.isFile() && file.getName().startsWith("access.log")) {
                readHttpdLog(file);
            }
        }
    }

    private void readHttpdLog(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String inLine;
        while ((inLine = in.readLine()) != null) {
            if (inLine.isEmpty()) continue;
            int spaceIndex = inLine.indexOf(' ', 20);
            if (inLine.charAt(20) != '/') {
                LOG.warn("Unknown line: {}", inLine);
            }
            String datasetName = inLine.substring(21, spaceIndex);

            int space2Index = inLine.indexOf(' ', spaceIndex+1);
            int statusCode = Integer.parseInt(inLine.substring(spaceIndex+1, space2Index));

            String extension = null;
            int dotIndex = datasetName.lastIndexOf('.');
            if (dotIndex != -1) {
                extension = datasetName.substring(dotIndex+1);
                if (extension.equals("tar")) {
                    // ok
                } else if (extension.equals("gz") || extension.equals("md5") || extension.equals("metadata")) {
                    datasetName = datasetName.substring(0, dotIndex);
                } else {
//                    LOG.debug("Unknown extension httpd: {}", extension);
                }
            }

            if (statusCode != 200 && statusCode != 304 && statusCode != 401) {// ok, notModified, auth required
                if (extension != null) {
                    get(httpdUnknownStatusCode, statusCode, k -> new TreeSet<>()).add(datasetName);
                }
                continue;
            }

            if (!datasetName.isEmpty()) {
                httpdLogDatasets.add(datasetName);
            }
        }
        in.close();

        // remove datasets that were found from list of 404
        for (Set<String> strings : httpdUnknownStatusCode.values()) {
            strings.removeAll(httpdLogDatasets);
        }
    }

    private void readGlobalProperties(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                readGlobalProperties(file);
            } else if (file.isFile() && file.getName().endsWith(".properties")) {
                readPropertiesFile(true, file, globalProperties);
            }
        }
    }

    private static final Pattern SUBST_PATTERN = Pattern.compile("\\$\\{(.+)\\}");
    private Set<String> readSubstitutedValues(Project project, String property) {
        if (property.contains("${")) {
            Matcher substMatcher = SUBST_PATTERN.matcher(property);
            if (substMatcher.find()) {
                String prop = substMatcher.group(1);
                Set<String> props = new HashSet<>();
                Set<String> strings = globalProperties.get(prop);
                if (strings != null) {
                    props.addAll(strings);
                }
                for (Project dependency : project.allDependencies) {
                    Set<String> strings1 = dependency.properties.get(prop);
                    if (strings1 != null) {
                        props.addAll(strings1);
                    }
                }
//                LOG.trace("Substitute {} -> {}", property, props);
                return props.stream().filter(s -> !s.isEmpty()).map(s -> substMatcher.replaceFirst(s)).collect(Collectors.toSet());
            }
        }

        return Collections.singleton(property);
    }

    private void addSpringFile(SpringFile springFile, Map<String, SpringFile> springFiles, Set<Project> projects) {
        for (String anImport : springFile.imports) {
            if (springFiles.containsKey(anImport)) continue;

            // Search imported file
            boolean importFound = false;
            for (Project dependentProject : projects) {
                SpringFile projectSpringFile = dependentProject.springFiles.get(anImport);
                if (projectSpringFile != null) {
                    if (springFiles.put(projectSpringFile.name, projectSpringFile) == null) {
                        addSpringFile(projectSpringFile, springFiles, projects);
                    }
                    importFound = true;
                    break;
                }
            }

            if (!importFound) {
                LOG.warn("Spring import not found {} from {}. Project {} -> {}", anImport, springFile.name, springFile.project.name, projects.stream().map(p -> p.name).collect(Collectors.toSet()));
            }
        }
        springFiles.put(springFile.name, springFile);
    }

    private void addProjectsDependencies(Set<Project> projects, Set<Project> dependencies) {
        for (Project dependency : dependencies) {
            if (projects.add(dependency)) {
                addProjectsDependencies(projects, dependency.dependencies);
            }
        }
    }

    private Project findProject(File projectRoot) {
        File pomFile = new File(projectRoot, "pom.xml");
        if (pomFile.isFile()) {
            return readProjectFile(projectRoot, pomFile);
        }
        return null;
    }

    private Project readProjectFile(File projectRoot, File pomFile) {
        LOG.trace("POM file: {}", pomFile);
        Element pomXml = parseXml(pomFile);
        Element projectArtifactIdXml = getElement(pomXml, "artifactId");
        String projectArtifactId = projectArtifactIdXml.getTextContent();

        Project project = getProject(projectArtifactId);
        project.pomFile = pomFile;
        project.pomXml = pomXml;
        Element dependenciesXml = getElement(pomXml, "dependencies");
        if (dependenciesXml != null) {
            for (Element dependencyXml : getElements(dependenciesXml, "dependency")) {
                Element groupIdXml = getElement(dependencyXml, "groupId");
                Element artifactIdXml = getElement(dependencyXml, "artifactId");
                if (groupIdXml.getTextContent().startsWith("com.contextweb")) {
                    String dependencyName = artifactIdXml.getTextContent();
                    project.dependencies.add(getProject(dependencyName));
                }
            }
        }
        Element parentArtifactIdXml = getElement(getElement(pomXml, "parent"), "artifactId");
        project.dependencies.add(getProject(parentArtifactIdXml.getTextContent()));

        findSpringFiles(project, new File(pomFile.getParent(), "src/main/resources"), 0);

        Element modules = getElement(pomXml, "modules");
        if (modules != null) {
            for (Element moduleNameXml : getElements(modules, "module")) {
                String moduleName = moduleNameXml.getTextContent();
                Project subProject = findProject(new File(projectRoot, moduleName));
                if (subProject == null) {
                    LOG.error("Subproject not found {} - {}", projectRoot, moduleName);
                } else {
                    project.modules.add(subProject);
                }
            }
        }
        return project;
    }

    private void findSpringFiles(Project project, File dir, int deep) {
        if (deep > MAX_PROJECT_SEARCH_DEEP || !dir.isDirectory() || new File(dir, "pom.xml").isFile()) return;
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                findSpringFiles(project, file, deep+1);
            } else if (file.isFile()) {
                if (file.getName().endsWith(".xml") && !file.getName().contains("log4j") && file.getName().toLowerCase().contains("context")) {
                    readSpringFile(project, file);
                } else if (file.getName().endsWith(".properties")) {
                    readPropertiesFile(false, project, file);
                }
            }
        }
    }

    private void readPropertiesFile(boolean globalProperties, Project project, File file) {
        readPropertiesFile(globalProperties, file, project.properties);
    }

    private void readPropertiesFile(boolean globalProperties, File file, Map<String, Set<String>> properties) {
        boolean datasetFile = globalProperties && file.getName().equals("dataservice.properties");
        Properties propertiesFile = new Properties();
        try (FileReader reader = new FileReader(file)) {
            propertiesFile.load(reader);
            for (Map.Entry<Object, Object> objectObjectEntry : propertiesFile.entrySet()) {
                String key = objectObjectEntry.getKey().toString();
                Set<String> strings = properties.get(key);
                if (strings == null) {
                    strings = new HashSet<>();
                    properties.put(key, strings);
                }
                String value = objectObjectEntry.getValue().toString();
                strings.add(value);
                if (datasetFile && key.startsWith("data.repository.") && key.endsWith(".dir")) {
                    datasetSrcFolders.add(value);
                }
            }
        } catch (Exception e) {
            LOG.error("Error reading properties file {}", file, e);
        }
    }

    private void readSpringFile(Project project, File file) {
//        LOG.trace("    Spring file {}", file);
        Element springXml = parseXml(file);
        if (springXml == null) return;

        // Parse imports
        SpringFile springFile = project.createSpringFile(file);
        NodeList anImport = springXml.getElementsByTagName("import");
        for (int i = 0; i < anImport.getLength(); i++) {
            Node importItem = anImport.item(i);
            Element importXml = (Element) importItem;
            String importName = importXml.getAttribute("resource");
            if (importName.startsWith("classpath:")) {
                importName = importName.substring(10);
            }
            // check properties substitution
            // todo

            springFile.imports.add(importName);
        }

        // Parse beans
        NodeList anBeans = springXml.getElementsByTagName("bean");
        for (int i = 0; i < anBeans.getLength(); i++) {
            Node beanItem = anBeans.item(i);
            Element beanXml = (Element) beanItem;
            springFile.createBean(beanXml);
        }


    }

    private void readRepositoryManager(SpringBean springBean) {
        Element repositoryFactoriesXml = springBean.getPropertyXml("repositoryFactories");
        if (repositoryFactoriesXml == null) {
            // Legacy
            repositoryFactoriesXml = springBean.getPropertyXml("repositories");
            if (repositoryFactoriesXml != null) {
                LOG.debug("Legacy repositories {}", springBean);
            } else {
                LOG.debug("Non list repository {}", springBean);
            }
        }

        if (repositoryFactoriesXml != null) {
            Set<String> beans = new HashSet<>();
            for (Element refXml : getElements(getElement(repositoryFactoriesXml, "list"), "ref")) {
                String beanName = refXml.getAttribute("bean");
                beans.add(beanName);
            }
            springBean.springFile.project.repositoryManagerBeans.addAll(beans);
        } else {
            springBean.springFile.project.noRepositoryManagerBeans = true;
        }
    }


    /*

    <bean id="userAgentOsMatchRuleRepository"
          class="com.contextweb.commons.data.repository.manager.RepositoryDefinition">
        <property name="source">
            <bean class="com.contextweb.commons.data.repository.manager.DataServiceRepositorySource" parent="baseRepositorySource">
                <property name="datasetName" value="userAgentOsMatchRules"/>
            </bean>
        </property>
        ...
    </bean>

     */
    private RepositoryBean readRepositoryDefinition(Project project, SpringBean springBean) {
        Element sourcePropertyXml = springBean.getPropertyXml("source");
        String ref = sourcePropertyXml.getAttribute("ref");
        SpringBean sourceBean;
        if (ref.length() == 0) {
            Element propertyBeanXml = getElement(sourcePropertyXml, "bean");
            sourceBean = new SpringBean(propertyBeanXml, springBean.springFile);
        } else {
            sourceBean = project.findSpringBean(ref);
        }

        Element datasetNamePropertyXml = sourceBean.getPropertyXml("datasetName");
        if (datasetNamePropertyXml == null) {
            LOG.error("No dataset property for bean {} in src {}", springBean, sourceBean);
            return null;
        }
        String dataSetName = datasetNamePropertyXml.getAttribute("value");
        if (dataSetName.length() > 0) {
            Set<String> datasets = readSubstitutedValues(project, dataSetName);
            RepositoryBean repositoryBean = new RepositoryBean(springBean, datasets);
            springBean.springFile.repositoryBeanMap.put(springBean.beanId, repositoryBean);
            return repositoryBean;
        } else {
            LOG.error("Empty value for dataset property for bean {} in src {}", springBean, sourceBean);
        }

        return null;
    }


    /*

    <bean id="domainBuilder" class="com.contextweb.commons.data.repository.MultiValueRepositoryDefinitionBuilder">
        <property name="repositoryFactory" ref="domainProbabilityDistributionFactory"/>
        <property name="dataSets" value="${domains.datasets}"/>
        <property name="dataServiceRepositorySourceSettings" ref="dataServiceRepositorySourceSettings"/>
    </bean>

     */
    private RepositoryBean readMultivalueRepositoryDefinition(Project project, SpringBean springBean) {
        Element datasetsPropertyXml = springBean.getPropertyXml("dataSets");
        String datasetsProperty = datasetsPropertyXml.getAttribute("value");
        Set<String> datasets = readSubstitutedValues(project, datasetsProperty).stream().flatMap(s -> Arrays.asList(s.split(",")).stream()).collect(Collectors.toSet());
        LOG.trace("Multivalue {}/{} -> {}", datasetsProperty, project.name, datasets);

        RepositoryBean repositoryBean = new RepositoryBean(springBean, datasets);
        springBean.springFile.repositoryBeanMap.put(springBean.beanId, repositoryBean);
        return repositoryBean;
    }


    private Element getElement(Element parent, String name) {
        List<Element> elements = getElements(parent, name);
        return elements.isEmpty() ? null : elements.get(0);
    }

    private List<Element> getElements(Element parent, String name) {
        if (parent == null) return Collections.emptyList();
        NodeList childNodes = parent.getChildNodes();
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) item;
                if (element.getTagName().equals(name)) {
                    elements.add(element);
                }
            }
        }
        return elements;
    }

    private Element parseXml(File file) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
            return doc.getDocumentElement();
        } catch (Exception e) {
            LOG.error("Error parsing xml {}", file, e);
        }
        return null;
    }


    private void findAllDataservice_ConfigurationsFiles() {
        File root = new File("/opt/projects/pulsepoint/dataservice-configurations");
        for (File file1 : root.listFiles()) {
            if (file1.isDirectory()) {
                findAllDataservice_ConfigurationsFiles(file1);
            }
        }
    }

    private void findAllDataservice_ConfigurationsFiles(File file) {
        String name = file.getName();
        if (name.equals(".git") || name.equals("README.md")) return;
        if (file.isFile()) {
            String dsname = name;
            String dsext = "";
            if (name.endsWith(".txt") || name.endsWith(".sql") || name.endsWith(".csv")) {
                dsname = name.substring(0, name.length() - 4);
                dsext = name.substring(name.length() - 3);
            }
            get(dataservicesConfigurations, dsname, k -> new ArrayList<>()).add(new DataserviceFile(dsname, dsext, file));
        }
        if (file.isDirectory()) {
            for (File file1 : file.listFiles()) {
                findAllDataservice_ConfigurationsFiles(file1);
            }
        }
    }

    private Project getProject(String name) {
        Project project = projects.get(name);
        if (project == null) {
            project = new Project(name);
            projects.put(name, project);
        }
        return project;
    }

    private class Project {
        private String name;
        private File pomFile;
        private Element pomXml;
        private Set<Project> dependencies = new HashSet<>();
        private Set<Project> allDependencies;
        private Map<String, SpringFile> springFiles = new HashMap<>();
        private boolean noRepositoryManagerBeans;
        private Set<String> repositoryManagerBeans = new HashSet<>();
        private Set<Project> modules = new HashSet<>();
        private Map<String, Set<String>> properties = new HashMap<>();

        private Collection<SpringFile> allSpringFiles;

        public Project(String name) {
            this.name = name;
        }

        public SpringFile createSpringFile(File file) {
            String name = file.getName();
            SpringFile springFile = new SpringFile(file, name, this);
            springFiles.put(name, springFile);
            return springFile;
        }

        public SpringBean findSpringBean(String beanId) {
            for (SpringFile springFile : allSpringFiles) {
                SpringBean springBean = springFile.springBeans.get(beanId);
                if (springBean != null) return springBean;
            }
            return null;
        }

        public RepositoryBean findRepositoryBean(String beanId) {
            for (SpringFile springFile : allSpringFiles) {
                RepositoryBean repositoryBean = springFile.repositoryBeanMap.get(beanId);
                if (repositoryBean != null) return repositoryBean;
            }
            LOG.warn("Repository bean {} not found in project {} in spring files {}", beanId, name, listSpringFiles());
            return null;
        }

        private Set<String> listSpringFiles() {
            return allSpringFiles.stream().map(s -> s.project.name + "/" + s.name).collect(Collectors.toSet());
        }
    }


    private class SpringFile {
        private File file;
        private String name;
        private Project project;
        private Set<String> imports = new HashSet<>();
        private Map<String, SpringBean> springBeans = new HashMap<>();
        private List<SpringBean> springBeansList = new ArrayList<>();
        private Map<String, RepositoryBean> repositoryBeanMap = new HashMap<>();

        public SpringFile(File file, String name, Project project) {
            this.file = file;
            this.name = name;
            this.project = project;
        }

        public SpringBean createBean(Element beanXml) {
            SpringBean bean = new SpringBean(beanXml, this);
//                LOG.trace("        Bean created {}", bean);
            springBeansList.add(bean);
            if (bean.beanId.length() > 0) {
                springBeans.put(bean.beanId, bean);
            }
            return bean;
        }
    }


    private class SpringBean {
        private String beanId;
        private String beanClass;
        private String beanParent;
        private SpringFile springFile;
        private Element beanXml;
        private boolean isAbstract;

        public SpringBean(Element beanXml, SpringFile springFile) {
            this.beanXml = beanXml;
            this.beanId = beanXml.getAttribute("id");
            if (this.beanId.length() == 0) {
                this.beanId = beanXml.getAttribute("name");
            }
            this.beanClass = beanXml.getAttribute("class");
            this.beanParent = beanXml.getAttribute("parent");
            isAbstract = "true".equals(beanXml.getAttribute("abstract"));
            this.springFile = springFile;
        }

        public String getBeanClass(Project project) {
            if (beanClass.length() == 0 && beanParent.length() > 0) {
                SpringBean parentBean = project.findSpringBean(beanParent);
                if (parentBean == null) {
                    LOG.error("Parent bean not found {} for {} in project {} files {}", beanParent, this, project.name, project.listSpringFiles());
                } else {
                    beanClass = parentBean.getBeanClass(project);
                }
            }
            return beanClass;
        }

        public Element getPropertyXml(String propertyName) {
            for (Element propertyXml : getElements(beanXml, "property")) {
                String name = propertyXml.getAttribute("name");
                if (name.equals(propertyName)) {
                    return propertyXml;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return springFile.project.name + "/" + springFile.name + "/" + beanId;
        }
    }

    private class RepositoryBean {
        private SpringBean springBean;
        private Set<String> datasets;
        private Set<Project> projectsUsed = new HashSet<>();


        public RepositoryBean(SpringBean springBean, Set<String> datasets) {
            this.springBean = springBean;
            this.datasets = datasets;
        }

        @Override
        public String toString() {
            return springBean.toString();
        }
    }

    private class DataserviceFile {
        private String name;
        private String type;
        private File file;

        public DataserviceFile(String name, String type, File file) {
            this.name = name;
            this.type = type;
            this.file = file;
        }
    }

    public static void main(String[] args) throws Exception {
        FindUsedDatasources finder = new FindUsedDatasources();
        finder.find();
    }

}
