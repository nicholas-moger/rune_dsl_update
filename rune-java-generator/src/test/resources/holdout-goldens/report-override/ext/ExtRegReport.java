package ext;

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
import ext.meta.ExtRegReportMeta;
import reg.RegReport;


/**
 * @version 0.0.0
 */
@RosettaDataType(value="ExtRegReport", builder=ExtRegReport.ExtRegReportBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ExtRegReport", model="ext", builder=ExtRegReport.ExtRegReportBuilderImpl.class, version="0.0.0")
public interface ExtRegReport extends RegReport {

	ExtRegReportMeta metaData = new ExtRegReportMeta();

	/*********************** Getter Methods  ***********************/

	/*********************** Build Methods  ***********************/
	ExtRegReport build();
	
	ExtRegReport.ExtRegReportBuilder toBuilder();
	
	static ExtRegReport.ExtRegReportBuilder builder() {
		return new ExtRegReport.ExtRegReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ExtRegReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ExtRegReport> getType() {
		return ExtRegReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("notation"), NotationEnum.class, getNotation(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ExtRegReportBuilder extends ExtRegReport, RegReport.RegReportBuilder {
		@Override
		ExtRegReport.ExtRegReportBuilder setNotation(NotationEnum notation);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("notation"), NotationEnum.class, getNotation(), this);
		}
		

		ExtRegReport.ExtRegReportBuilder prune();
	}

	/*********************** Immutable Implementation of ExtRegReport  ***********************/
	class ExtRegReportImpl extends RegReport.RegReportImpl implements ExtRegReport {
		
		protected ExtRegReportImpl(ExtRegReport.ExtRegReportBuilder builder) {
			super(builder);
		}
		
		@Override
		public ExtRegReport build() {
			return this;
		}
		
		@Override
		public ExtRegReport.ExtRegReportBuilder toBuilder() {
			ExtRegReport.ExtRegReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ExtRegReport.ExtRegReportBuilder builder) {
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
			return "ExtRegReport {" +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of ExtRegReport  ***********************/
	class ExtRegReportBuilderImpl extends RegReport.RegReportBuilderImpl implements ExtRegReport.ExtRegReportBuilder {
	
		
		@RosettaAttribute("notation")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notation")
		@Override
		public ExtRegReport.ExtRegReportBuilder setNotation(NotationEnum _notation) {
			this.notation = _notation == null ? null : _notation;
			return this;
		}
		
		@Override
		public ExtRegReport build() {
			return new ExtRegReport.ExtRegReportImpl(this);
		}
		
		@Override
		public ExtRegReport.ExtRegReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ExtRegReport.ExtRegReportBuilder prune() {
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
		public ExtRegReport.ExtRegReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			ExtRegReport.ExtRegReportBuilder o = (ExtRegReport.ExtRegReportBuilder) other;
			
			
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
			return "ExtRegReportBuilder {" +
			'}' + " " + super.toString();
		}
	}
}
