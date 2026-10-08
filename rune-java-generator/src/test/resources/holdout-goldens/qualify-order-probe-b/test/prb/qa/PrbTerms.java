package test.prb.qa;

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
import java.util.Objects;
import test.prb.qa.meta.PrbTermsMeta;

import static java.util.Optional.ofNullable;

/**
 * The qualifiable root type of test.prb.qa.
 * @version 0.0.0
 */
@RosettaDataType(value="PrbTerms", builder=PrbTerms.PrbTermsBuilderImpl.class, version="0.0.0")
@RuneDataType(value="PrbTerms", model="test", builder=PrbTerms.PrbTermsBuilderImpl.class, version="0.0.0")
public interface PrbTerms extends RosettaModelObject {

	PrbTermsMeta metaData = new PrbTermsMeta();

	/*********************** Getter Methods  ***********************/
	String getKind();

	/*********************** Build Methods  ***********************/
	PrbTerms build();
	
	PrbTerms.PrbTermsBuilder toBuilder();
	
	static PrbTerms.PrbTermsBuilder builder() {
		return new PrbTerms.PrbTermsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends PrbTerms> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends PrbTerms> getType() {
		return PrbTerms.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface PrbTermsBuilder extends PrbTerms, RosettaModelObjectBuilder {
		PrbTerms.PrbTermsBuilder setKind(String kind);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
		}
		

		PrbTerms.PrbTermsBuilder prune();
	}

	/*********************** Immutable Implementation of PrbTerms  ***********************/
	class PrbTermsImpl implements PrbTerms {
		private final String kind;
		
		protected PrbTermsImpl(PrbTerms.PrbTermsBuilder builder) {
			this.kind = builder.getKind();
		}
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@Override
		public PrbTerms build() {
			return this;
		}
		
		@Override
		public PrbTerms.PrbTermsBuilder toBuilder() {
			PrbTerms.PrbTermsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(PrbTerms.PrbTermsBuilder builder) {
			ofNullable(getKind()).ifPresent(builder::setKind);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PrbTerms _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PrbTerms {" +
				"kind=" + this.kind +
			'}';
		}
	}

	/*********************** Builder Implementation of PrbTerms  ***********************/
	class PrbTermsBuilderImpl implements PrbTerms.PrbTermsBuilder {
	
		protected String kind;
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@RosettaAttribute("kind")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kind")
		@Override
		public PrbTerms.PrbTermsBuilder setKind(String _kind) {
			this.kind = _kind == null ? null : _kind;
			return this;
		}
		
		@Override
		public PrbTerms build() {
			return new PrbTerms.PrbTermsImpl(this);
		}
		
		@Override
		public PrbTerms.PrbTermsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PrbTerms.PrbTermsBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKind()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PrbTerms.PrbTermsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			PrbTerms.PrbTermsBuilder o = (PrbTerms.PrbTermsBuilder) other;
			
			
			merger.mergeBasic(getKind(), o.getKind(), this::setKind);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PrbTerms _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PrbTermsBuilder {" +
				"kind=" + this.kind +
			'}';
		}
	}
}
