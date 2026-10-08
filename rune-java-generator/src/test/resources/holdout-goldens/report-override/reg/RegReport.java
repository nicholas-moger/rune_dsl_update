package reg;

import cde.layer.price.NotationEnum;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import common.layer.CommonReport;
import reg.meta.RegReportMeta;


/**
 * @version 0.0.0
 */
@RosettaDataType(value="RegReport", builder=RegReport.RegReportBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RegReport", model="reg", builder=RegReport.RegReportBuilderImpl.class, version="0.0.0")
public interface RegReport extends CommonReport {

	RegReportMeta metaData = new RegReportMeta();

	/*********************** Getter Methods  ***********************/

	/*********************** Build Methods  ***********************/
	RegReport build();
	
	RegReport.RegReportBuilder toBuilder();
	
	static RegReport.RegReportBuilder builder() {
		return new RegReport.RegReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RegReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RegReport> getType() {
		return RegReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("notation"), NotationEnum.class, getNotation(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RegReportBuilder extends RegReport, CommonReport.CommonReportBuilder {
		@Override
		RegReport.RegReportBuilder setNotation(NotationEnum notation);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("notation"), NotationEnum.class, getNotation(), this);
		}
		

		RegReport.RegReportBuilder prune();
	}

	/*********************** Immutable Implementation of RegReport  ***********************/
	class RegReportImpl extends CommonReport.CommonReportImpl implements RegReport {
		
		protected RegReportImpl(RegReport.RegReportBuilder builder) {
			super(builder);
		}
		
		@Override
		public RegReport build() {
			return this;
		}
		
		@Override
		public RegReport.RegReportBuilder toBuilder() {
			RegReport.RegReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RegReport.RegReportBuilder builder) {
			super.setBuilderFields(builder);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
		
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			return _result;
		}
		
		@Override
		public String toString() {
			return "RegReport {" +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of RegReport  ***********************/
	class RegReportBuilderImpl extends CommonReport.CommonReportBuilderImpl implements RegReport.RegReportBuilder {
	
		
		@RosettaAttribute("notation")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notation")
		@Override
		public RegReport.RegReportBuilder setNotation(NotationEnum _notation) {
			this.notation = _notation == null ? null : _notation;
			return this;
		}
		
		@Override
		public RegReport build() {
			return new RegReport.RegReportImpl(this);
		}
		
		@Override
		public RegReport.RegReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RegReport.RegReportBuilder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RegReport.RegReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			RegReport.RegReportBuilder o = (RegReport.RegReportBuilder) other;
			
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
		
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			return _result;
		}
		
		@Override
		public String toString() {
			return "RegReportBuilder {" +
			'}' + " " + super.toString();
		}
	}
}
