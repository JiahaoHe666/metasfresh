-- 2017-04-22T14:03:11.488
-- URL zum Konzept
INSERT INTO AD_Sequence (AD_Client_ID,AD_Org_ID,AD_Sequence_ID,Created,CreatedBy,CurrentNext,CurrentNextSys,Description,IncrementNo,IsActive,IsAudited,IsAutoSequence,IsTableID,Name,StartNewYear,StartNo,Updated,UpdatedBy) VALUES (1000000,0,554389,TO_TIMESTAMP('2017-04-22 14:03:11','YYYY-MM-DD HH24:MI:SS'),100,1000000,50000,'DocumentNo/Value for Table C_Payment',1,'Y','N','Y','N','Zahlungsausgang','N',1000000,TO_TIMESTAMP('2017-04-22 14:03:11','YYYY-MM-DD HH24:MI:SS'),100)
;

-- 2017-04-22T14:03:21.510
-- URL zum Konzept
INSERT INTO AD_Sequence (AD_Client_ID,AD_Org_ID,AD_Sequence_ID,Created,CreatedBy,CurrentNext,CurrentNextSys,Description,IncrementNo,IsActive,IsAudited,IsAutoSequence,IsTableID,Name,StartNewYear,StartNo,Updated,UpdatedBy) VALUES (1000000,0,554390,TO_TIMESTAMP('2017-04-22 14:03:21','YYYY-MM-DD HH24:MI:SS'),100,1000000,50000,'DocumentNo/Value for Table C_Payment',1,'Y','N','Y','N','Zahlungseingang','N',1000000,TO_TIMESTAMP('2017-04-22 14:03:21','YYYY-MM-DD HH24:MI:SS'),100)
;

-- 2017-04-22T14:03:38.907
-- URL zum Konzept
UPDATE AD_Sequence SET Name='Payment Incoming',Updated=TO_TIMESTAMP('2017-04-22 14:03:38','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Sequence_ID=554390
;

-- 2017-04-22T14:03:44.811
-- URL zum Konzept
UPDATE AD_Sequence SET Name='Payment Outgoing',Updated=TO_TIMESTAMP('2017-04-22 14:03:44','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Sequence_ID=554389
;

-- 2017-04-22T14:04:06.970
-- URL zum Konzept
UPDATE AD_Sequence SET CurrentNext=2000000,Updated=TO_TIMESTAMP('2017-04-22 14:04:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Sequence_ID=554389
;

-- 2017-04-22T14:04:27.790
-- URL zum Konzept
UPDATE C_DocType SET DocNoSequence_ID=554390, IsDocNoControlled='Y',Updated=TO_TIMESTAMP('2017-04-22 14:04:27','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE C_DocType_ID=1000008
;

-- 2017-04-22T14:04:39.635
-- URL zum Konzept
UPDATE C_DocType SET DocNoSequence_ID=554389, IsDocNoControlled='Y',Updated=TO_TIMESTAMP('2017-04-22 14:04:39','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE C_DocType_ID=1000009
;

