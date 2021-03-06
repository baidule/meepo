
package org.feisoft.jta.logging.deserializer;

import org.feisoft.jta.supports.resource.CommonResourceDescriptor;
import org.feisoft.jta.supports.resource.LocalXAResourceDescriptor;
import org.feisoft.jta.supports.resource.RemoteResourceDescriptor;
import org.feisoft.jta.supports.resource.UnidentifiedResourceDescriptor;
import org.feisoft.transaction.TransactionBeanFactory;
import org.feisoft.transaction.archive.XAResourceArchive;
import org.feisoft.transaction.aware.TransactionBeanFactoryAware;
import org.feisoft.transaction.logging.ArchiveDeserializer;
import org.feisoft.transaction.supports.resource.XAResourceDescriptor;
import org.feisoft.transaction.supports.serialize.XAResourceDeserializer;
import org.feisoft.transaction.xa.TransactionXid;
import org.feisoft.transaction.xa.XidFactory;

import javax.transaction.xa.Xid;
import java.nio.ByteBuffer;

public class XAResourceArchiveDeserializer implements ArchiveDeserializer, TransactionBeanFactoryAware {

	@javax.inject.Inject
	private TransactionBeanFactory beanFactory;
	// private XAResourceDeserializer deserializer;

	public byte[] serialize(TransactionXid xid, Object obj) {
		XAResourceArchive archive = (XAResourceArchive) obj;

		Xid branchXid = archive.getXid();
		byte[] branchQualifier = branchXid.getBranchQualifier();

		XAResourceDescriptor descriptor = archive.getDescriptor();
		byte[] identifierByteArray = new byte[0];
		byte typeByte = 0x0;
		if (CommonResourceDescriptor.class.isInstance(descriptor)) {
			typeByte = (byte) 0x1;
			identifierByteArray = descriptor.getIdentifier().getBytes();
		} else if (RemoteResourceDescriptor.class.isInstance(descriptor)) {
			typeByte = (byte) 0x2;
			identifierByteArray = descriptor.getIdentifier().getBytes();
		} else if (LocalXAResourceDescriptor.class.isInstance(descriptor)) {
			typeByte = (byte) 0x3;
			identifierByteArray = descriptor.getIdentifier().getBytes();
		}

		byte branchVote = (byte) archive.getVote();
		byte readonly = archive.isReadonly() ? (byte) 1 : (byte) 0;
		byte committed = archive.isCommitted() ? (byte) 1 : (byte) 0;
		byte rolledback = archive.isRolledback() ? (byte) 1 : (byte) 0;
		byte completed = archive.isCompleted() ? (byte) 1 : (byte) 0;
		byte heuristic = archive.isHeuristic() ? (byte) 1 : (byte) 0;

		byte[] byteArray = new byte[XidFactory.BRANCH_QUALIFIER_LENGTH + 2 + identifierByteArray.length + 6];
		System.arraycopy(branchQualifier, 0, byteArray, 0, branchQualifier.length);

		byteArray[XidFactory.BRANCH_QUALIFIER_LENGTH] = typeByte;
		byteArray[XidFactory.BRANCH_QUALIFIER_LENGTH + 1] = (byte) identifierByteArray.length;
		if (identifierByteArray.length > 0) {
			System.arraycopy(identifierByteArray, 0, byteArray, XidFactory.BRANCH_QUALIFIER_LENGTH + 2,
					identifierByteArray.length);
		}

		byteArray[XidFactory.BRANCH_QUALIFIER_LENGTH + 2 + identifierByteArray.length] = branchVote;
		byteArray[XidFactory.BRANCH_QUALIFIER_LENGTH + 2 + identifierByteArray.length + 1] = readonly;
		byteArray[XidFactory.BRANCH_QUALIFIER_LENGTH + 2 + identifierByteArray.length + 2] = committed;
		byteArray[XidFactory.BRANCH_QUALIFIER_LENGTH + 2 + identifierByteArray.length + 3] = rolledback;
		byteArray[XidFactory.BRANCH_QUALIFIER_LENGTH + 2 + identifierByteArray.length + 4] = completed;
		byteArray[XidFactory.BRANCH_QUALIFIER_LENGTH + 2 + identifierByteArray.length + 5] = heuristic;

		return byteArray;
	}

	public Object deserialize(TransactionXid xid, byte[] array) {
		XAResourceDeserializer deserializer = this.beanFactory.getResourceDeserializer();

		ByteBuffer buffer = ByteBuffer.wrap(array);

		XAResourceArchive archive = new XAResourceArchive();

		byte[] branchQualifier = new byte[XidFactory.BRANCH_QUALIFIER_LENGTH];
		buffer.get(branchQualifier);
		XidFactory xidFactory = this.beanFactory.getXidFactory();
		TransactionXid branchXid = xidFactory.createBranchXid(xid, branchQualifier);
		archive.setXid(branchXid);

		byte resourceType = buffer.get();
		byte length = buffer.get();
		byte[] byteArray = new byte[length];
		buffer.get(byteArray);
		String identifier = new String(byteArray);

		XAResourceDescriptor descriptor = null;
		if (resourceType == 0x01) {
			archive.setIdentified(true);
			descriptor = deserializer.deserialize(identifier);
		} else if (resourceType == 0x02) {
			archive.setIdentified(true);
			descriptor = deserializer.deserialize(identifier);
		} else if (resourceType == 0x03) {
			archive.setIdentified(true);
			descriptor = deserializer.deserialize(identifier);
		} else {
			descriptor = new UnidentifiedResourceDescriptor();
		}

		if (CommonResourceDescriptor.class.isInstance(descriptor)) {
			((CommonResourceDescriptor) descriptor).setRecoverXid(branchXid);
		}

		archive.setDescriptor(descriptor);

		int branchVote = buffer.get();
		int readonly = buffer.get();
		int committedValue = buffer.get();
		int rolledbackValue = buffer.get();
		int completedValue = buffer.get();
		int heuristicValue = buffer.get();
		archive.setVote(branchVote);
		archive.setReadonly(readonly != 0);
		archive.setCommitted(committedValue != 0);
		archive.setRolledback(rolledbackValue != 0);
		archive.setCompleted(completedValue != 0);
		archive.setHeuristic(heuristicValue != 0);

		return archive;
	}

	public void setBeanFactory(TransactionBeanFactory tbf) {
		this.beanFactory = tbf;
	}

}
