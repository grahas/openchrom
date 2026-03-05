/*******************************************************************************
 * Copyright (c) 2026 Lablicate GmbH.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.csd.converter.supplier.axr.fragment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.processing.core.IProcessingInfo;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import net.openchrom.csd.converter.supplier.axr.converter.ChromatogramImportConverter;

@TestInstance(Lifecycle.PER_CLASS)
public class AXRMissingX_ITest {

	private IChromatogramCSD chromatogram;

	@BeforeAll
	public void setUp() {

		File file = new File(TestPathHelper.MISSING_X_AXR);
		ChromatogramImportConverter importConverter = new ChromatogramImportConverter();
		IProcessingInfo<IChromatogramCSD> processingInfo = importConverter.convert(file, new NullProgressMonitor());
		chromatogram = processingInfo.getProcessingResult();
	}

	@Test
	public void testLoadChromatogram() {

		assertNotNull(chromatogram);
		assertEquals(4, chromatogram.getNumberOfScans());
	}

	@Test
	public void testMissingXRetentionFallback() {

		assertEquals(0, chromatogram.getScan(1).getRetentionTime());
		assertEquals(60, chromatogram.getScan(2).getRetentionTime());
		assertEquals(120, chromatogram.getScan(3).getRetentionTime());
		assertEquals(180, chromatogram.getScan(4).getRetentionTime());
		assertEquals(130f, chromatogram.getScan(4).getTotalSignal(), 0);
	}
}
