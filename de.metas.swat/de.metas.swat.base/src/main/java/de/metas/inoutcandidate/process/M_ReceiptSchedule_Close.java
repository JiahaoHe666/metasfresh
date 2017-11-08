package de.metas.inoutcandidate.process;

import java.util.Iterator;

import org.adempiere.ad.dao.ConstantQueryFilter;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryFilter;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Services;
import org.apache.commons.collections4.IteratorUtils;
import org.compiere.util.TrxRunnable;

import de.metas.inoutcandidate.api.IReceiptScheduleBL;
import de.metas.inoutcandidate.model.I_M_ReceiptSchedule;
import de.metas.process.IProcessPrecondition;
import de.metas.process.IProcessPreconditionsContext;
import de.metas.process.JavaProcess;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.process.RunOutOfTrx;
import lombok.NonNull;

/**
 * Close receipt schedule line.
 *
 * This is counter-part of {@link M_ReceiptSchedule_ReOpen}.
 *
 * @author tsa
 * @task http://dewiki908/mediawiki/index.php/08480_Korrekturm%C3%B6glichkeit_Wareneingang_-_Menge%2C_Packvorschrift%2C_Merkmal_%28109195602347%29
 *
 */
public class M_ReceiptSchedule_Close extends JavaProcess implements IProcessPrecondition
{
	private static final String MSG_RECEIPT_SCHEDULES_ALL_CLOSED = "M_ReceiptSchedule_Close.ReceiptSchedulesAllClosed";
	private static final String MSG_SKIP_CLOSED_1P = "M_ReceiptSchedule_Close.SkipClosed_1P";

	private final transient IReceiptScheduleBL receiptScheduleBL = Services.get(IReceiptScheduleBL.class);
	private final transient IQueryBL queryBL = Services.get(IQueryBL.class);

	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable(@NonNull final IProcessPreconditionsContext context)
	{
		if (context.isNoSelection())
		{
			return ProcessPreconditionsResolution.rejectBecauseNoSelection();
		}

		// Make sure at least one receipt schedule is open
		final boolean someSchedsAreStillOpen = context.getSelectedModels(I_M_ReceiptSchedule.class).stream()
				.anyMatch(receiptSchedule -> !receiptScheduleBL.isClosed(receiptSchedule));

		if (!someSchedsAreStillOpen)
		{
			return ProcessPreconditionsResolution.reject(msgBL.getTranslatableMsgText(MSG_RECEIPT_SCHEDULES_ALL_CLOSED));
		}

		return ProcessPreconditionsResolution.accept();
	}

	@Override
	@RunOutOfTrx
	protected String doIt() throws Exception
	{
		final IQueryFilter<I_M_ReceiptSchedule> selectedSchedsFilter = getProcessInfo().getQueryFilterOrElse(ConstantQueryFilter.of(false));

		final Iterator<I_M_ReceiptSchedule> scheds = queryBL.createQueryBuilder(I_M_ReceiptSchedule.class)
				.addOnlyActiveRecordsFilter()
				.filter(selectedSchedsFilter)
				.create()
				.iterate(I_M_ReceiptSchedule.class);

		int counter = 0;
		for (final I_M_ReceiptSchedule receiptSchedule : IteratorUtils.asIterable(scheds))
		{
			if (receiptScheduleBL.isClosed(receiptSchedule))
			{
				addLog(msgBL.getMsg(getCtx(), MSG_SKIP_CLOSED_1P, new Object[] { receiptSchedule.getM_ReceiptSchedule_ID() }));
				continue;
			}
			closeInTrx(receiptSchedule);
			counter++;
		}

		return "@Processed@: " + counter;
	}

	private void closeInTrx(final I_M_ReceiptSchedule receiptSchedule)
	{
		Services.get(ITrxManager.class)
				.run(new TrxRunnable()
				{
					@Override
					public void run(String localTrxName) throws Exception
					{
						InterfaceWrapperHelper.setThreadInheritedTrxName(receiptSchedule);
						receiptScheduleBL.close(receiptSchedule);
					}
				});
	}
}
