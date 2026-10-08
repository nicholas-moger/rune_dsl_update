package common.layer;

import cde.layer.CriticalDataElement;
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
import common.layer.meta.CommonReportMeta;


/**
 * @version 0.0.0
 */
@RosettaDataType(value="CommonReport", builder=CommonReport.CommonReportBuilderImpl.class, version="0.0.0")
@RuneDataType(value="CommonReport", model="common", builder=CommonReport.CommonReportBuilderImpl.class, version="0.0.0")
public interface CommonReport extends CriticalDataElement {

	CommonReportMeta metaData = new CommonReportMeta();

	/*********************** Getter Methods  ***********************/

	/*********************** Build Methods  ***********************/
	CommonReport build();
	
	CommonReport.CommonReportBuilder toBuilder();
	
	static CommonReport.CommonReportBuilder builder() {
		return new CommonReport.CommonReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends CommonReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends CommonReport> getType() {
		return CommonReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("notation"), NotationEnum.class, getNotation(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CommonReportBuilder extends CommonReport, CriticalDataElement.CriticalDataElementBuilder {
		@Override
		CommonReport.CommonReportBuilder setNotation(NotationEnum notation);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("notation"), NotationEnum.class, getNotation(), this);
		}
		

		CommonReport.CommonReportBuilder prune();
	}

	/*********************** Immutable Implementation of CommonReport  ***********************/
	class CommonReportImpl extends CriticalDataElement.CriticalDataElementImpl implements CommonReport {
		
		protected CommonReportImpl(CommonReport.CommonReportBuilder builder) {
			super(builder);
		}
		
		@Override
		public CommonReport build() {
			return this;
		}
		
		@Override
		public CommonReport.CommonReportBuilder toBuilder() {
			CommonReport.CommonReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(CommonReport.CommonReportBuilder builder) {
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
			return "CommonReport {" +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of CommonReport  ***********************/
	class CommonReportBuilderImpl extends CriticalDataElement.CriticalDataElementBuilderImpl implements CommonReport.CommonReportBuilder {
	
		
		@RosettaAttribute("notation")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notation")
		@Override
		public CommonReport.CommonReportBuilder setNotation(NotationEnum _notation) {
			this.notation = _notation == null ? null : _notation;
			return this;
		}
		
		@Override
		public CommonReport build() {
			return new CommonReport.CommonReportImpl(this);
		}
		
		@Override
		public CommonReport.CommonReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public CommonReport.CommonReportBuilder prune() {
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
		public CommonReport.CommonReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			CommonReport.CommonReportBuilder o = (CommonReport.CommonReportBuilder) other;
			
			
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
			return "CommonReportBuilder {" +
			'}' + " " + super.toString();
		}
	}
}
