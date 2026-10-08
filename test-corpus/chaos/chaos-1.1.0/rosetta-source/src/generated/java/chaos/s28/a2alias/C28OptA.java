package chaos.s28.a2alias;

import chaos.s28.a2alias.meta.C28OptAMeta;
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

import static java.util.Optional.ofNullable;

/**
 * Choice option A - the F15&#39; implicit-input chain head.
 * @version 1.0.0
 */
@RosettaDataType(value="C28OptA", builder=C28OptA.C28OptABuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28OptA", model="chaos", builder=C28OptA.C28OptABuilderImpl.class, version="1.0.0")
public interface C28OptA extends RosettaModelObject {

	C28OptAMeta metaData = new C28OptAMeta();

	/*********************** Getter Methods  ***********************/
	String getAv();

	/*********************** Build Methods  ***********************/
	C28OptA build();
	
	C28OptA.C28OptABuilder toBuilder();
	
	static C28OptA.C28OptABuilder builder() {
		return new C28OptA.C28OptABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28OptA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28OptA> getType() {
		return C28OptA.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28OptABuilder extends C28OptA, RosettaModelObjectBuilder {
		C28OptA.C28OptABuilder setAv(String av);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("av"), String.class, getAv(), this);
		}
		

		C28OptA.C28OptABuilder prune();
	}

	/*********************** Immutable Implementation of C28OptA  ***********************/
	class C28OptAImpl implements C28OptA {
		private final String av;
		
		protected C28OptAImpl(C28OptA.C28OptABuilder builder) {
			this.av = builder.getAv();
		}
		
		@Override
		@RosettaAttribute("av")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("av")
		public String getAv() {
			return av;
		}
		
		@Override
		public C28OptA build() {
			return this;
		}
		
		@Override
		public C28OptA.C28OptABuilder toBuilder() {
			C28OptA.C28OptABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28OptA.C28OptABuilder builder) {
			ofNullable(getAv()).ifPresent(builder::setAv);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28OptA _that = getType().cast(o);
		
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
			return "C28OptA {" +
				"av=" + this.av +
			'}';
		}
	}

	/*********************** Builder Implementation of C28OptA  ***********************/
	class C28OptABuilderImpl implements C28OptA.C28OptABuilder {
	
		protected String av;
		
		@Override
		@RosettaAttribute("av")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("av")
		public String getAv() {
			return av;
		}
		
		@RosettaAttribute("av")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("av")
		@Override
		public C28OptA.C28OptABuilder setAv(String _av) {
			this.av = _av == null ? null : _av;
			return this;
		}
		
		@Override
		public C28OptA build() {
			return new C28OptA.C28OptAImpl(this);
		}
		
		@Override
		public C28OptA.C28OptABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28OptA.C28OptABuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAv()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28OptA.C28OptABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28OptA.C28OptABuilder o = (C28OptA.C28OptABuilder) other;
			
			
			merger.mergeBasic(getAv(), o.getAv(), this::setAv);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28OptA _that = getType().cast(o);
		
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
			return "C28OptABuilder {" +
				"av=" + this.av +
			'}';
		}
	}
}
