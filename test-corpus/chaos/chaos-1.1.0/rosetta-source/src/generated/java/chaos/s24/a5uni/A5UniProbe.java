package chaos.s24.a5uni;

import chaos.s24.a5uni.meta.A5UniProbeMeta;
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
 * Non-ASCII literal probe – müßig 試験.
 * @version 1.0.0
 */
@RosettaDataType(value="A5UniProbe", builder=A5UniProbe.A5UniProbeBuilderImpl.class, version="1.0.0")
@RuneDataType(value="A5UniProbe", model="chaos", builder=A5UniProbe.A5UniProbeBuilderImpl.class, version="1.0.0")
public interface A5UniProbe extends RosettaModelObject {

	A5UniProbeMeta metaData = new A5UniProbeMeta();

	/*********************** Getter Methods  ***********************/
	String getP();

	/*********************** Build Methods  ***********************/
	A5UniProbe build();
	
	A5UniProbe.A5UniProbeBuilder toBuilder();
	
	static A5UniProbe.A5UniProbeBuilder builder() {
		return new A5UniProbe.A5UniProbeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends A5UniProbe> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends A5UniProbe> getType() {
		return A5UniProbe.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface A5UniProbeBuilder extends A5UniProbe, RosettaModelObjectBuilder {
		A5UniProbe.A5UniProbeBuilder setP(String p);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
		}
		

		A5UniProbe.A5UniProbeBuilder prune();
	}

	/*********************** Immutable Implementation of A5UniProbe  ***********************/
	class A5UniProbeImpl implements A5UniProbe {
		private final String p;
		
		protected A5UniProbeImpl(A5UniProbe.A5UniProbeBuilder builder) {
			this.p = builder.getP();
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		public A5UniProbe build() {
			return this;
		}
		
		@Override
		public A5UniProbe.A5UniProbeBuilder toBuilder() {
			A5UniProbe.A5UniProbeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(A5UniProbe.A5UniProbeBuilder builder) {
			ofNullable(getP()).ifPresent(builder::setP);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A5UniProbe _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A5UniProbe {" +
				"p=" + this.p +
			'}';
		}
	}

	/*********************** Builder Implementation of A5UniProbe  ***********************/
	class A5UniProbeBuilderImpl implements A5UniProbe.A5UniProbeBuilder {
	
		protected String p;
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@RosettaAttribute("p")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("p")
		@Override
		public A5UniProbe.A5UniProbeBuilder setP(String _p) {
			this.p = _p == null ? null : _p;
			return this;
		}
		
		@Override
		public A5UniProbe build() {
			return new A5UniProbe.A5UniProbeImpl(this);
		}
		
		@Override
		public A5UniProbe.A5UniProbeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public A5UniProbe.A5UniProbeBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getP()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public A5UniProbe.A5UniProbeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			A5UniProbe.A5UniProbeBuilder o = (A5UniProbe.A5UniProbeBuilder) other;
			
			
			merger.mergeBasic(getP(), o.getP(), this::setP);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A5UniProbe _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A5UniProbeBuilder {" +
				"p=" + this.p +
			'}';
		}
	}
}
