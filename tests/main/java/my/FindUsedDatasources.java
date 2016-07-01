package my;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parse spring structure
 *
 * @author Pavel Moukhataev
 */
public class FindUsedDatasources {

    private static final Logger LOG = LogManager.getLogger();

//    private static final String ROOT_LOCATION = "/opt/projects/pulsepoint";
    private static final String ROOT_LOCATION = "/Public/Projects/PulsePoint/github";

    private static final String[] dataserviceDirs = {
        /*
/Batch/automated-datasets
/Web/BH
/Web/CDS/Predict/ActiveClickTrees
/Web/CDSLogger
/Web/DataServices
*/
"dashboards/sql/",
"dashboards/text/",
"dataAggregator",
"datascience/datasets/sql",
"datascience/datasets/text",
"datascience/sql",
"datascience/text",
"datateam/file/",
"datateam/sql/",
"fraud",
"mpc-support",
"quality-service",
"sql",
"text",
"xt3"            
    };

    private static final int MAX_PROJECT_SEARCH_DEEP = 4;



    private Map<String, Project> projects = new HashMap<>();
    private Map<String, DataserviceFile> existedDataservices = new HashMap<>();
    private Set<String> existedHttpDataservices = new HashSet<>();
    private Map<String, Set<String>> globalProperties = new HashMap<>();



    private void find() throws Exception {
        // Cycle through projects to find all spring files for this project
        findProject(new File(ROOT_LOCATION + "/ad-serving-and-commons"));
        readGlobalProperties(new File(ROOT_LOCATION + "/ad-serving-configuration"));

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
        findAllFiles();
        findAllHttpFiles();

        LOG.debug("Dataset files [{}]: {}", existedDataservices.size(), existedDataservices.keySet());
        LOG.debug("Http files [{}]: {}", existedHttpDataservices.size(), existedHttpDataservices);

        LOG.debug("Implicitly listed beans: {}", repositoryBeanList.stream().filter(b -> !explicitlyListedRepositoryBeanSet.contains(b)).collect(Collectors.toSet()));
        LOG.debug("Unused beans: {}", repositoryBeanList.stream().filter(r -> r.projectsUsed.isEmpty()).collect(Collectors.toSet()));

        LOG.debug("Not found beans datasets: {}", datasetRepositoryBeanMap.entrySet().stream().filter(k -> !existedDataservices.containsKey(k.getKey())).flatMap(d -> d.getValue().stream()).collect(Collectors.toSet()));

        Set<File> unusedBySpringDatasets = existedDataservices.entrySet().stream().filter(d -> !datasetRepositoryBeanMap.containsKey(d.getKey())).map(d -> d.getValue().file).collect(Collectors.toSet());
        LOG.debug("Unused bySpring datasets[{}]: {}", unusedBySpringDatasets.size(), unusedBySpringDatasets);
        Set<File> unusedByHttpDatasets = existedDataservices.entrySet().stream().filter(d -> !existedHttpDataservices.contains(d.getKey())).map(d -> d.getValue().file).collect(Collectors.toSet());
        LOG.debug("Unused http datasets [{}]: {}", unusedByHttpDatasets.size(), unusedByHttpDatasets);
        Set<File> unusedBothDatasets = new HashSet<>(unusedBySpringDatasets);
        unusedBothDatasets.retainAll(unusedByHttpDatasets);
        LOG.debug("Unused in both datasets[{}]: {}", unusedBothDatasets.size(), unusedBothDatasets);
    }

    private void readGlobalProperties(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                readGlobalProperties(file);
            } else if (file.isFile() && file.getName().endsWith(".properties")) {
                readPropertiesFile(file, globalProperties);
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
                    readPropertiesFile(project, file);
                }
            }
        }
    }

    private void readPropertiesFile(Project project, File file) {
        readPropertiesFile(file, project.properties);
    }

    private void readPropertiesFile(File file, Map<String, Set<String>> properties) {
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
                strings.add(objectObjectEntry.getValue().toString());
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
        List<Element> elements = new ArrayList<>();
        NodeList childNodes = parent.getChildNodes();
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


    private void findAllFiles() throws IOException {
        File root = new File(ROOT_LOCATION + "/dataservice-configurations");
        for (String dataserviceDir : dataserviceDirs) {
            File dir = new File(root, dataserviceDir);
            File[] files = dir.listFiles();
            if (files == null) {
                LOG.error("Unknown dir: " + dir.getCanonicalPath());
            } else {
                for (File file : files) {
                    findAllFiles(file, false);
                }
            }
        }
    }

    private void findAllFiles(File file, boolean subdirs) {
        String name = file.getName();
        if (name.equals(".git") || name.equals("README.md")) return;
        if (file.isFile()) {
            String dsname = name;
            String dsext = "";
            if (name.endsWith(".txt") || name.endsWith(".sql") || name.endsWith(".csv")) {
                dsname = name.substring(0, name.length() - 4);
                dsext = name.substring(name.length() - 3);
            }
            existedDataservices.put(dsname, new DataserviceFile(dsname, dsext, file));
        }
        if (subdirs) {
            if (file.isDirectory()) {
                for (File file1 : file.listFiles()) {
                    findAllFiles(file1, subdirs);
                }
            }
        }
    }

    private void findAllHttpFiles() throws Exception {
        File httpFiles = new File(ROOT_LOCATION, "pp-internals/test-data/src/misc/ET-70-UnusedDatasets/http-access");
        for (File dir : httpFiles.listFiles()) {
            for (File file : dir.listFiles(f -> f.getName().startsWith("access.log"))) {
                LineNumberReader in = new LineNumberReader(new FileReader(file));
                String inString;
                while ((inString = in.readLine()) != null) {
                    String[] split = inString.split(" ");
                    String fileName = split[2];
                    if (fileName.endsWith(".metadata") || fileName.endsWith(".md5")) continue;
                    if (fileName.endsWith(".gz")) {
                        fileName = fileName.substring(0, fileName.length() - 3);
                    }
                    fileName = fileName.substring(1);
                    if (fileName.length() > 0) {
                        existedHttpDataservices.add(fileName);
                    }
                }
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
            return beanId + "/" + springFile.name + "/" + springFile.project.name;
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
