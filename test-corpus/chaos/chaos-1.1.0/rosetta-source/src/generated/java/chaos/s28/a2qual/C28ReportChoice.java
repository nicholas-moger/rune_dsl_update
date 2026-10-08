package chaos.s28.a2qual;

import chaos.s28.a2qual.meta.C28ReportChoiceMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * A report `with type` naming a CHOICE - the released plugin resolves it (Choice extends Data), the fork&#39;s RDataType gate refuses at TYPE_NOT_FOUND (seat 4&#39;s banked report-withtype-choice).
 * @version 1.0.0
 */
@RosettaDataType(value="C28ReportChoice", builder=C28ReportChoice.C28ReportChoiceBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28ReportChoice", model="chaos", builder=C28ReportChoice.C28ReportChoiceBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C28ReportChoice extends RosettaModelObject {

	C28ReportChoiceMeta metaData = new C28ReportChoiceMeta();

	/*********************** Getter Methods  ***********************/
	C28ChoiceReport getC28ChoiceReport();
	C28Report getC28Report();

	/*********************** Build Methods  ***********************/
	C28ReportChoice build();
	
	C28ReportChoice.C28ReportChoiceBuilder toBuilder();
	
	static C28ReportChoice.C28ReportChoiceBuilder builder() {
		return new C28ReportChoice.C28ReportChoiceBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28ReportChoice> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28ReportChoice> getType() {
		return C28ReportChoice.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C28ChoiceReport"), processor, C28ChoiceReport.class, getC28ChoiceReport());
		processRosetta(path.newSubPath("C28Report"), processor, C28Report.class, getC28Report());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28ReportChoiceBuilder extends C28ReportChoice, RosettaModelObjectBuilder {
		C28ChoiceReport.C28ChoiceReportBuilder getOrCreateC28ChoiceReport();
		@Override
		C28ChoiceReport.C28ChoiceReportBuilder getC28ChoiceReport();
		C28Report.C28ReportBuilder getOrCreateC28Report();
		@Override
		C28Report.C28ReportBuilder getC28Report();
		C28ReportChoice.C28ReportChoiceBuilder setC28ChoiceReport(C28ChoiceReport _C28ChoiceReport);
		C28ReportChoice.C28ReportChoiceBuilder setC28Report(C28Report _C28Report);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C28ChoiceReport"), processor, C28ChoiceReport.C28ChoiceReportBuilder.class, getC28ChoiceReport());
			processRosetta(path.newSubPath("C28Report"), processor, C28Report.C28ReportBuilder.class, getC28Report());
		}
		

		C28ReportChoice.C28ReportChoiceBuilder prune();
	}

	/*********************** Immutable Implementation of C28ReportChoice  ***********************/
	class C28ReportChoiceImpl implements C28ReportChoice {
		private final C28ChoiceReport c28ChoiceReport;
		private final C28Report c28Report;
		
		protected C28ReportChoiceImpl(C28ReportChoice.C28ReportChoiceBuilder builder) {
			this.c28ChoiceReport = ofNullable(builder.getC28ChoiceReport()).map(f->f.build()).orElse(null);
			this.c28Report = ofNullable(builder.getC28Report()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C28ChoiceReport")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C28ChoiceReport")
		public C28ChoiceReport getC28ChoiceReport() {
			return c28ChoiceReport;
		}
		
		@Override
		@RosettaAttribute("C28Report")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C28Report")
		public C28Report getC28Report() {
			return c28Report;
		}
		
		@Override
		public C28ReportChoice build() {
			return this;
		}
		
		@Override
		public C28ReportChoice.C28ReportChoiceBuilder toBuilder() {
			C28ReportChoice.C28ReportChoiceBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28ReportChoice.C28ReportChoiceBuilder builder) {
			ofNullable(getC28ChoiceReport()).ifPresent(builder::setC28ChoiceReport);
			ofNullable(getC28Report()).ifPresent(builder::setC28Report);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28ReportChoice _that = getType().cast(o);
		
			if (!Objects.equals(c28ChoiceReport, _that.getC28ChoiceReport())) return false;
			if (!Objects.equals(c28Report, _that.getC28Report())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c28ChoiceReport != null ? c28ChoiceReport.hashCode() : 0);
			_result = 31 * _result + (c28Report != null ? c28Report.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28ReportChoice {" +
				"C28ChoiceReport=" + this.c28ChoiceReport + ", " +
				"C28Report=" + this.c28Report +
			'}';
		}
	}

	/*********************** Builder Implementation of C28ReportChoice  ***********************/
	class C28ReportChoiceBuilderImpl implements C28ReportChoice.C28ReportChoiceBuilder {
	
		protected C28ChoiceReport.C28ChoiceReportBuilder c28ChoiceReport;
		protected C28Report.C28ReportBuilder c28Report;
		
		@Override
		@RosettaAttribute("C28ChoiceReport")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C28ChoiceReport")
		public C28ChoiceReport.C28ChoiceReportBuilder getC28ChoiceReport() {
			return c28ChoiceReport;
		}
		
		@Override
		public C28ChoiceReport.C28ChoiceReportBuilder getOrCreateC28ChoiceReport() {
			C28ChoiceReport.C28ChoiceReportBuilder result;
			if (c28ChoiceReport!=null) {
				result = c28ChoiceReport;
			}
			else {
				result = c28ChoiceReport = C28ChoiceReport.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C28Report")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C28Report")
		public C28Report.C28ReportBuilder getC28Report() {
			return c28Report;
		}
		
		@Override
		public C28Report.C28ReportBuilder getOrCreateC28Report() {
			C28Report.C28ReportBuilder result;
			if (c28Report!=null) {
				result = c28Report;
			}
			else {
				result = c28Report = C28Report.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C28ChoiceReport")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C28ChoiceReport")
		@Override
		public C28ReportChoice.C28ReportChoiceBuilder setC28ChoiceReport(C28ChoiceReport _c28ChoiceReport) {
			this.c28ChoiceReport = _c28ChoiceReport == null ? null : _c28ChoiceReport.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C28Report")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C28Report")
		@Override
		public C28ReportChoice.C28ReportChoiceBuilder setC28Report(C28Report _c28Report) {
			this.c28Report = _c28Report == null ? null : _c28Report.toBuilder();
			return this;
		}
		
		@Override
		public C28ReportChoice build() {
			return new C28ReportChoice.C28ReportChoiceImpl(this);
		}
		
		@Override
		public C28ReportChoice.C28ReportChoiceBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28ReportChoice.C28ReportChoiceBuilder prune() {
			if (c28ChoiceReport!=null && !c28ChoiceReport.prune().hasData()) c28ChoiceReport = null;
			if (c28Report!=null && !c28Report.prune().hasData()) c28Report = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC28ChoiceReport()!=null && getC28ChoiceReport().hasData()) return true;
			if (getC28Report()!=null && getC28Report().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28ReportChoice.C28ReportChoiceBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28ReportChoice.C28ReportChoiceBuilder o = (C28ReportChoice.C28ReportChoiceBuilder) other;
			
			merger.mergeRosetta(getC28ChoiceReport(), o.getC28ChoiceReport(), this::setC28ChoiceReport);
			merger.mergeRosetta(getC28Report(), o.getC28Report(), this::setC28Report);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28ReportChoice _that = getType().cast(o);
		
			if (!Objects.equals(c28ChoiceReport, _that.getC28ChoiceReport())) return false;
			if (!Objects.equals(c28Report, _that.getC28Report())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c28ChoiceReport != null ? c28ChoiceReport.hashCode() : 0);
			_result = 31 * _result + (c28Report != null ? c28Report.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28ReportChoiceBuilder {" +
				"C28ChoiceReport=" + this.c28ChoiceReport + ", " +
				"C28Report=" + this.c28Report +
			'}';
		}
	}
}
