/*******************************************************************************
 * Copyright (c) 2026 Lablicate GmbH.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.wsd.converter.supplier.axr.ui.editors;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.chemclipse.processing.core.IProcessingMessage;
import org.eclipse.chemclipse.model.types.DataType;
import org.eclipse.chemclipse.processing.core.IProcessingInfo;
import org.eclipse.chemclipse.ux.extension.ui.provider.ISupplierEditorSupport;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.EditorSupportFactory;
import org.eclipse.chemclipse.wsd.converter.chromatogram.ChromatogramConverterWSD;
import org.eclipse.chemclipse.wsd.model.core.IChromatogramWSD;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import net.openchrom.wsd.converter.supplier.axr.Activator;

public class EditorAxr extends EditorPart {

	private static final Logger logger = Logger.getLogger(EditorAxr.class);
	private static final String TITLE = "AXR Chromatogram";
	private static final String ERROR_TITLE = "AXR Import";
	private static final DataType[] CHROMATOGRAM_EDITOR_TYPES = new DataType[]{DataType.WSD, DataType.CSD};

	private IEditorInput editorInput;
	private Label statusLabel;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {

		setSite(site);
		setInput(input);
		editorInput = input;
		setPartName(input != null ? input.getName() : TITLE);
	}

	@Override
	public void createPartControl(Composite parent) {

		statusLabel = new Label(parent, SWT.WRAP);
		statusLabel.setText("Importing AXR chromatogram...");
		parent.getDisplay().asyncExec(this::importAndOpen);
	}

	@Override
	public void setFocus() {

		if(statusLabel != null && !statusLabel.isDisposed()) {
			statusLabel.setFocus();
		}
	}

	@Override
	public void doSave(org.eclipse.core.runtime.IProgressMonitor monitor) {

		// Not supported.
	}

	@Override
	public void doSaveAs() {

		// Not supported.
	}

	@Override
	public boolean isDirty() {

		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {

		return false;
	}

	private void importAndOpen() {

		File file = resolveFile(editorInput);
		if(file == null) {
			showError("Could not resolve AXR file path from editor input.");
			return;
		}
		IProcessingInfo<IChromatogramWSD> processingInfo = ChromatogramConverterWSD.getInstance().convert(file, new NullProgressMonitor());
		if(processingInfo == null || processingInfo.hasErrorMessages() || processingInfo.getProcessingResult() == null) {
			showError("Failed to import AXR file: " + file.getAbsolutePath() + collectProcessingMessages(processingInfo));
			return;
		}
		try {
			openImportedChromatogram(processingInfo.getProcessingResult());
			IEditorPart thisEditor = this;
			if(getSite() != null && getSite().getPage() != null) {
				getSite().getPage().closeEditor(thisEditor, false);
			}
		} catch(Exception e) {
			logger.warn(e);
			showError("AXR import succeeded, but opening the chromatogram editor failed: " + e.getMessage());
		}
	}

	private void openImportedChromatogram(IChromatogramWSD chromatogram) {

		@SuppressWarnings({"rawtypes", "unchecked"})
		Supplier context = () -> Activator.getDefault() != null ? Activator.getDefault().getEclipseContext() : null;
		List<String> errors = new ArrayList<>();
		for(DataType dataType : CHROMATOGRAM_EDITOR_TYPES) {
			try {
				ISupplierEditorSupport editorSupport = new EditorSupportFactory(dataType, context).getInstanceEditorSupport();
				editorSupport.openEditor(chromatogram);
				return;
			} catch(Exception e) {
				errors.add(dataType + ": " + e.getMessage());
				logger.warn(e);
			}
		}
		throw new IllegalStateException("No chromatogram editor accepted the imported AXR data. " + String.join(" | ", errors));
	}

	private File resolveFile(IEditorInput input) {

		if(input == null) {
			return null;
		}
		if(input instanceof IPathEditorInput pathEditorInput) {
			IPath path = pathEditorInput.getPath();
			if(path != null) {
				return path.toFile();
			}
		}
		if(input instanceof IURIEditorInput uriEditorInput) {
			URI uri = uriEditorInput.getURI();
			if(uri != null && "file".equalsIgnoreCase(uri.getScheme())) {
				return new File(uri);
			}
		}
		IFile workspaceFile = input.getAdapter(IFile.class);
		if(workspaceFile != null && workspaceFile.getLocation() != null) {
			return workspaceFile.getLocation().toFile();
		}
		return null;
	}

	private void showError(String message) {

		if(statusLabel != null && !statusLabel.isDisposed()) {
			statusLabel.setText(message);
		}
		if(getSite() != null && getSite().getShell() != null) {
			MessageDialog.openError(getSite().getShell(), ERROR_TITLE, message);
		}
	}

	private String collectProcessingMessages(IProcessingInfo<IChromatogramWSD> processingInfo) {

		if(processingInfo == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for(IProcessingMessage message : processingInfo.getMessages()) {
			if(message == null || message.getDescription() == null || message.getDescription().isBlank()) {
				continue;
			}
			if(builder.isEmpty()) {
				builder.append("\n");
			}
			builder.append("- ").append(message.getDescription()).append("\n");
		}
		return builder.toString();
	}
}
