package chaos.s18.a2alias;

import chaos.s18.a2alias.meta.C18OptAMeta;
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
 * Choice-guard option A.
 * @version 1.0.0
 */
@RosettaDataType(value="C18OptA", builder=C18OptA.C18OptABuilderImpl.class, version="1.0.0")
@RuneDataType(value="C18OptA", model="chaos", builder=C18OptA.C18OptABuilderImpl.class, version="1.0.0")
public interface C18OptA extends RosettaModelObject {

	C18OptAMeta metaData = new C18OptAMeta();

	/*********************** Getter Methods  ***********************/
	String getAv();

	/*********************** Build Methods  ***********************/
	C18OptA build();
	
	C18OptA.C18OptABuilder toBuilder();
	
	static C18OptA.C18OptABuilder builder() {
		return new C18OptA.C18OptABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C18OptA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C18OptA> getType() {
		return C18OptA.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C18OptABuilder extends C18OptA, RosettaModelObjectBuilder {
		C18OptA.C18OptABuilder setAv(String av);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
		}
		

		C18OptA.C18OptABuilder prune();
	}

	/*********************** Immutable Implementation of C18OptA  ***********************/
	class C18OptAImpl implements C18OptA {
		private final String av;
		
		protected C18OptAImpl(C18OptA.C18OptABuilder builder) {
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
		public C18OptA build() {
			return this;
		}
		
		@Override
		public C18OptA.C18OptABuilder toBuilder() {
			C18OptA.C18OptABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C18OptA.C18OptABuilder builder) {
			ofNullable(getAv()).ifPresent(builder::setAv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18OptA _that = getType().cast(o);
		
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
			return "C18OptA {" +
				"av=" + this.av +
			'}';
		}
	}

	/*********************** Builder Implementation of C18OptA  ***********************/
	class C18OptABuilderImpl implements C18OptA.C18OptABuilder {
	
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
		public C18OptA.C18OptABuilder setAv(String _av) {
			this.av = _av == null ? null : _av;
			return this;
		}
		
		@Override
		public C18OptA build() {
			return new C18OptA.C18OptAImpl(this);
		}
		
		@Override
		public C18OptA.C18OptABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18OptA.C18OptABuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18OptA.C18OptABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C18OptA.C18OptABuilder o = (C18OptA.C18OptABuilder) other;
			
			
			merger.mergeBasic(getAv(), o.getAv(), this::setAv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18OptA _that = getType().cast(o);
		
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
			return "C18OptABuilder {" +
				"av=" + this.av +
			'}';
		}
	}
}
