package chaos.s26.a1o2;

import chaos.s26.a1o2.meta.C26OptAMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
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

import static java.util.Optional.ofNullable;

/**
 * Choice option A.
 * @version 1.0.0
 */
@RosettaDataType(value="C26OptA", builder=C26OptA.C26OptABuilderImpl.class, version="1.0.0")
@RuneDataType(value="C26OptA", model="chaos", builder=C26OptA.C26OptABuilderImpl.class, version="1.0.0")
public interface C26OptA extends RosettaModelObject {

	C26OptAMeta metaData = new C26OptAMeta();

	/*********************** Getter Methods  ***********************/
	String getAv();

	/*********************** Build Methods  ***********************/
	C26OptA build();
	
	C26OptA.C26OptABuilder toBuilder();
	
	static C26OptA.C26OptABuilder builder() {
		return new C26OptA.C26OptABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C26OptA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C26OptA> getType() {
		return C26OptA.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C26OptABuilder extends C26OptA, RosettaModelObjectBuilder {
		C26OptA.C26OptABuilder setAv(String av);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
		}
		

		C26OptA.C26OptABuilder prune();
	}

	/*********************** Immutable Implementation of C26OptA  ***********************/
	class C26OptAImpl implements C26OptA {
		private final String av;
		
		protected C26OptAImpl(C26OptA.C26OptABuilder builder) {
			this.av = builder.getAv();
		}
		
		@Override
		@RosettaAttribute("av")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("av")
		public String getAv() {
			return av;
		}
		
		@Override
		public C26OptA build() {
			return this;
		}
		
		@Override
		public C26OptA.C26OptABuilder toBuilder() {
			C26OptA.C26OptABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C26OptA.C26OptABuilder builder) {
			ofNullable(getAv()).ifPresent(builder::setAv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26OptA _that = getType().cast(o);
		
			if (!Objects.equals(av, _that.getAv())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (av != null ? av.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26OptA {" +
				"av=" + this.av +
			'}';
		}
	}

	/*********************** Builder Implementation of C26OptA  ***********************/
	class C26OptABuilderImpl implements C26OptA.C26OptABuilder {
	
		protected String av;
		
		@Override
		@RosettaAttribute("av")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("av")
		public String getAv() {
			return av;
		}
		
		@RosettaAttribute("av")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("av")
		@Override
		public C26OptA.C26OptABuilder setAv(String _av) {
			this.av = _av == null ? null : _av;
			return this;
		}
		
		@Override
		public C26OptA build() {
			return new C26OptA.C26OptAImpl(this);
		}
		
		@Override
		public C26OptA.C26OptABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26OptA.C26OptABuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26OptA.C26OptABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C26OptA.C26OptABuilder o = (C26OptA.C26OptABuilder) other;
			
			
			merger.mergeBasic(getAv(), o.getAv(), this::setAv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26OptA _that = getType().cast(o);
		
			if (!Objects.equals(av, _that.getAv())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (av != null ? av.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26OptABuilder {" +
				"av=" + this.av +
			'}';
		}
	}
}
