package cn.gmlee.plugin.ui;

import cn.gmlee.plugin.constants.MsgValue;
import cn.gmlee.plugin.service.CodeGenerateService;
import cn.gmlee.plugin.service.TableInfoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.database.model.DasNamespace;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.psi.DbTable;
import com.intellij.icons.AllIcons;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Conditions;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.JBIterable;
import cn.gmlee.plugin.config.Settings;
import cn.gmlee.plugin.entity.TableInfo;
import cn.gmlee.plugin.entity.Template;
import cn.gmlee.plugin.entity.TemplateGroup;
import cn.gmlee.plugin.tool.CloneUtils;
import cn.gmlee.plugin.tool.CollectionUtil;
import cn.gmlee.plugin.tool.ProjectUtils;
import cn.gmlee.plugin.ui.base.BaseGroupPanel;
import cn.gmlee.plugin.ui.base.BaseItemSelectPanel;
import cn.gmlee.plugin.ui.base.TemplateEditor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ?????????????????????
 *
 * @author Jas??
 * @version 1.0.0
 * @since 2018/07/18 09:33
 */
public class TemplateSettingPanel implements Configurable {
    /**
     * ?????????????????????????????????
     */
    private static final String TEMPLATE_DESCRIPTION_INFO;

    static {
        String descriptionInfo = "";
        try {
            descriptionInfo = UrlUtil.loadText(TemplateSettingPanel.class.getResource("/description/templateDescription.html"));
        } catch (IOException e) {
            ExceptionUtil.rethrow(e);
        } finally {
            TEMPLATE_DESCRIPTION_INFO = descriptionInfo;
        }
    }

    /**
     * ????????????
     */
    private Settings settings;

    /**
     * ???????????????
     */
    private TemplateEditor templateEditor;

    /**
     * ?????????????????????
     */
    private BaseGroupPanel baseGroupPanel;

    /**
     * ???????????????????????????
     */
    private BaseItemSelectPanel<Template> baseItemSelectPanel;

    /**
     * ????????????
     */
    private Map<String, TemplateGroup> group;

    /**
     * ??????????????????
     */
    private String currGroupName;

    /**
     * ????????????
     */
    private Project project;

    TemplateSettingPanel() {
        // ????????????
        this.project = ProjectUtils.getCurrProject();
        // ?????????????????????
        this.settings = Settings.getInstance();
        // ????????????
        this.currGroupName = this.settings.getCurrTemplateGroupName();
        this.group = CloneUtils.cloneByJson(this.settings.getTemplateGroupMap(), new TypeReference<Map<String, TemplateGroup>>() {});
    }

    /**
     * ???????????????????????????
     *
     * @return ??????
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "Template Setting";
    }

    /**
     * Returns the topic in the help file which is shown when help for the configurable is requested.
     *
     * @return the help topic, or {@code null} if no help is available
     */
    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    /**
     * ?????????????????????
     *
     * @return ???????????????
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        // ???????????????
        JPanel mainPanel = new JPanel(new BorderLayout());

        this.currGroupName = findExistedGroupName(this.currGroupName);

        // ?????????????????????
        this.baseGroupPanel = new BaseGroupPanel(new ArrayList<>(group.keySet()), this.currGroupName) {
            @Override
            protected void createGroup(String name) {
                // ????????????
                TemplateGroup templateGroup = new TemplateGroup();
                templateGroup.setName(name);
                templateGroup.setElementList(new ArrayList<>());
                group.put(name, templateGroup);
                currGroupName = name;
                baseGroupPanel.reset(new ArrayList<>(group.keySet()), currGroupName);
                // ??????????????????????????????????????????
                templateEditor.reset("empty", "");
            }

            @Override
            protected void deleteGroup(String name) {
                // ????????????
                group.remove(name);
                currGroupName = Settings.DEFAULT_NAME;
                baseGroupPanel.reset(new ArrayList<>(group.keySet()), currGroupName);
            }

            @Override
            protected void copyGroup(String name) {
                // ????????????
                TemplateGroup templateGroup = CloneUtils.cloneByJson(group.get(currGroupName));
                templateGroup.setName(name);
                currGroupName = name;
                group.put(name, templateGroup);
                baseGroupPanel.reset(new ArrayList<>(group.keySet()), currGroupName);
            }

            @Override
            protected void changeGroup(String name) {
                currGroupName = name;
                if (baseItemSelectPanel == null) {
                    return;
                }
                // ??????????????????
                baseItemSelectPanel.reset(group.get(currGroupName).getElementList(), 0);
                if (group.get(currGroupName).getElementList().isEmpty()) {
                    // ???????????????????????????????????????
                    templateEditor.reset("empty", "");
                }
            }
        };

        // ????????????????????????
        this.baseItemSelectPanel = new BaseItemSelectPanel<Template>(group.get(currGroupName).getElementList(), true) {
            @Override
            protected void addItem(String name) {
                List<Template> templateList = group.get(currGroupName).getElementList();
                // ????????????
                templateList.add(new Template(name, ""));
                baseItemSelectPanel.reset(templateList, templateList.size() - 1);
            }

            @Override
            protected void copyItem(String newName, Template item) {
                // ????????????
                Template template = CloneUtils.cloneByJson(item);
                template.setName(newName);
                List<Template> templateList = group.get(currGroupName).getElementList();
                templateList.add(template);
                baseItemSelectPanel.reset(templateList, templateList.size() - 1);
            }

            @Override
            protected void deleteItem(Template item) {
                // ????????????
                group.get(currGroupName).getElementList().remove(item);
                baseItemSelectPanel.reset(group.get(currGroupName).getElementList(), 0);
                if (group.get(currGroupName).getElementList().isEmpty()) {
                    // ???????????????????????????????????????
                    templateEditor.reset("empty", "");
                }
            }

            @Override
            protected void selectedItem(Template item) {
                // ??????????????????????????????????????????????????????????????????
                if (templateEditor == null) {
                    // ???????????????velocity???IDE????????????
                    String fileType = "vm";
                    if (FileTypeManager.getInstance().getFileTypeByExtension(fileType) == UnknownFileType.INSTANCE) {
                        fileType = "txt";
                    }
                    FileType velocityFileType = FileTypeManager.getInstance().getFileTypeByExtension(fileType);
                    templateEditor = new TemplateEditor(project, item.getName() + "." + fileType, item.getCode(), TEMPLATE_DESCRIPTION_INFO, velocityFileType);
                    // ??????????????????
                    templateEditor.setCallback(() -> onUpdate());
                    baseItemSelectPanel.getRightPanel().add(templateEditor.createComponent(), BorderLayout.CENTER);
                } else {
                    // ????????????
                    templateEditor.reset(item.getName(), item.getCode());
                }
            }
        };

        // ??????????????????
        this.addDebugPanel();

        mainPanel.add(baseGroupPanel, BorderLayout.NORTH);
        mainPanel.add(baseItemSelectPanel.getComponent(), BorderLayout.CENTER);
        return mainPanel;
    }

    /**
     * ????????????????????????
     *
     * @param groupName ?????????
     * @return ??????????????????
     */
    private String findExistedGroupName(String groupName) {
        //??????groupName?????????
        if (!group.containsKey(groupName)) {
            if (group.containsKey(Settings.DEFAULT_NAME)) {//????????????????????????
                return Settings.DEFAULT_NAME;
            } else {
                //?????????????????????
                return group.keySet().stream().findFirst().orElse(Settings.DEFAULT_NAME);
            }
        }
        return groupName;
    }

    private JBIterable<DasTable> getTables(DbDataSource dataSource) {
        return dataSource.getModel().traverser().expandAndSkip(Conditions.instanceOf(DasNamespace.class)).filter(DasTable.class);
    }

    /**
     * ??????????????????
     */
    private void addDebugPanel() {
        // ?????????
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(new JLabel("????????????"));

        // ???????????????
        List<String> tableList = new ArrayList<>();
        List<DbDataSource> dataSourceList = DbPsiFacade.getInstance(project).getDataSources();
        if (!CollectionUtil.isEmpty(dataSourceList)) {
            dataSourceList.forEach(dbDataSource -> getTables(dbDataSource).forEach(table -> tableList.add(table.toString())));
        }
        ComboBoxModel<String> comboBoxModel = new CollectionComboBoxModel<>(tableList);
        ComboBox<String> comboBox = new ComboBox<>(comboBoxModel);
        panel.add(comboBox);

        // ??????????????????
        DefaultActionGroup actionGroup = new DefaultActionGroup(new AnAction(AllIcons.Debugger.Console) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                // ??????????????????
                String name = (String) comboBox.getSelectedItem();
                List<DbDataSource> dataSourceList = DbPsiFacade.getInstance(project).getDataSources();
                DasTable dasTable = null;
                if (!CollectionUtil.isEmpty(dataSourceList)) {
                    for (DbDataSource dbDataSource : dataSourceList) {
                        for (DasTable table : getTables(dbDataSource)) {
                            if (Objects.equals(table.toString(), name)) {
                                dasTable = table;
                            }
                        }
                    }
                }
                if (dasTable == null) {
                    return;
                }
                DbTable dbTable = null;
                if (dasTable instanceof DbTable) {
                    // ??????2017.2???????????????
                    dbTable = (DbTable) dasTable;
                } else {
                    Method method = ReflectionUtil.getMethod(DbPsiFacade.class, "findElement", DasObject.class);
                    if (method == null) {
                        Messages.showWarningDialog("findElement method not found", MsgValue.TITLE_INFO);
                        return;
                    }
                    try {
                        // ??????2017.2?????????????????????
                        dbTable = (DbTable) method.invoke(DbPsiFacade.getInstance(project), dasTable);
                    } catch (IllegalAccessException|InvocationTargetException e1) {
                        ExceptionUtil.rethrow(e1);
                    }
                }
                // ???????????????
                TableInfo tableInfo = TableInfoService.getInstance(project).getTableInfoAndConfig(dbTable);
                // ??????????????????????????????????????????
                if (tableInfo.getSavePackageName() == null) {
                    tableInfo.setSavePackageName("com.companyname.modulename");
                }
                // ????????????
                String code = CodeGenerateService.getInstance(project).generate(new Template("temp", templateEditor.getEditor().getDocument().getText()), tableInfo);

                // ???????????????
                EditorFactory editorFactory = EditorFactory.getInstance();
                PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
                String fileName = templateEditor.getName();
                FileType velocityFileType = FileTypeManager.getInstance().getFileTypeByExtension("vm");
                // ???IDE?????????velocity??????????????????txt??????????????????
                if (velocityFileType == UnknownFileType.INSTANCE) {
                    velocityFileType = FileTypeManager.getInstance().getFileTypeByExtension("txt");
                }
                PsiFile psiFile = psiFileFactory.createFileFromText("JasCodeTemplateDebug.vm.ft", velocityFileType, code, 0, true);
                // ?????????????????????velocity??????????????????
                psiFile.getViewProvider().putUserData(FileTemplateManager.DEFAULT_TEMPLATE_PROPERTIES, FileTemplateManager.getInstance(project).getDefaultProperties());
                Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                assert document != null;
                Editor editor = editorFactory.createEditor(document, project, velocityFileType, true);
                // ???????????????
                EditorSettings editorSettings = editor.getSettings();
                // ??????????????????
                editorSettings.setVirtualSpace(false);
                // ????????????????????????????????????
                editorSettings.setLineMarkerAreaShown(false);
                // ??????????????????
                editorSettings.setIndentGuidesShown(false);
                // ????????????
                editorSettings.setLineNumbersShown(true);
                // ??????????????????
                editorSettings.setFoldingOutlineShown(true);
                // ???????????????????????????????????????
                editorSettings.setAdditionalColumnsCount(3);
                editorSettings.setAdditionalLinesCount(3);
                // ?????????????????????
                editorSettings.setCaretRowShown(false);
                ((EditorEx) editor).setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, new LightVirtualFile(fileName)));
                // ??????dialog
                DialogBuilder dialogBuilder = new DialogBuilder(project);
                dialogBuilder.setTitle(MsgValue.TITLE_INFO);
                JComponent component = editor.getComponent();
                component.setPreferredSize(new Dimension(800, 600));
                dialogBuilder.setCenterPanel(component);
                dialogBuilder.addCloseButton();
                dialogBuilder.addDisposable(() -> {
                    //??????????????????
                    editorFactory.releaseEditor(editor);
                    dialogBuilder.dispose();
                });
                dialogBuilder.show();
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(comboBox.getSelectedItem() != null);
            }
        });
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Template Debug", actionGroup, true);
        panel.add(actionToolbar.getComponent());
        baseGroupPanel.add(panel, BorderLayout.EAST);
    }

    /**
     * ???????????????????????????
     */
    private void onUpdate() {
        // ?????????????????????
        Template template = baseItemSelectPanel.getSelectedItem();
        if (template != null) {
            template.setCode(templateEditor.getEditor().getDocument().getText());
        }
    }

    /**
     * ?????????????????????
     *
     * @return ???????????????
     */
    @Override
    public boolean isModified() {
        return !settings.getTemplateGroupMap().equals(group) || !settings.getCurrTemplateGroupName().equals(currGroupName);
    }

    /**
     * ????????????
     */
    @Override
    public void apply() {
        settings.setTemplateGroupMap(group);
        settings.setCurrTemplateGroupName(currGroupName);
    }

    /**
     * ????????????
     */
    @Override
    public void reset() {
        // ???????????????????????????????????????
        if (!isModified()) {
            return;
        }
        // ???????????????????????????????????????
        this.group = CloneUtils.cloneByJson(settings.getTemplateGroupMap(), new TypeReference<Map<String, TemplateGroup>>() {});
        this.currGroupName = settings.getCurrTemplateGroupName();
        this.currGroupName = findExistedGroupName(settings.getCurrTemplateGroupName());
        if (baseGroupPanel == null) {
            return;
        }
        // ????????????????????????
        baseGroupPanel.reset(new ArrayList<>(group.keySet()), currGroupName);
    }

    /**
     * ??????????????????
     */
    @Override
    public void disposeUIResources() {
        // ???????????????
        if (templateEditor != null) {
            templateEditor.onClose();
        }
    }
}
